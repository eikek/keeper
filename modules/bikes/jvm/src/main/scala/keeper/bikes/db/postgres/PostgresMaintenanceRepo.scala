package keeper.bikes.db.postgres

import java.time.Instant

import cats.effect.{Resource, Sync}
import cats.syntax.all.*
import fs2.Stream

import keeper.bikes.Page
import keeper.bikes.db.MaintenanceRepository
import keeper.bikes.model._
import keeper.bikes.service.TranslatedEvent
import keeper.common.Distance
import keeper.core.*

import skunk.data.Completion
import skunk.{Session, Void}

final class PostgresMaintenanceRepo[F[_]: Sync](session: Resource[F, Session[F]])
    extends MaintenanceRepository[F] {
  private[this] val logger = scribe.cats.effect[F]

  def getServiceDetails(search: ServiceSearchMask): Stream[F, ServiceDetail] = {
    val frag = SearchSql.searchQueryFragment(search)
    val q = frag.fragment.query(SearchSql.resultCodec)
    Stream
      .resource(session)
      .flatMap(s => Stream.eval(s.prepare(q)).flatMap(_.stream(frag.argument, 50)))
  }

  def findBikeServices(until: Option[Instant], page: Page): F[List[BikeService]] =
    val all =
      until match
        case Some(d) =>
          session.use(_.execute(MaintenanceSql.findAllBikeServicesPagedUntil)(d -> page))

        case None =>
          session.use(_.execute(MaintenanceSql.findAllBikeServicesPaged)(page))

    session.use { s =>
      all.flatMap(_.traverse { case (id, svc) =>
        (
          s.execute(MaintenanceSql.findServiceEvents)(id),
          s.execute(MaintenanceSql.selectTotals)(id)
        ).mapN { (sc, tot) =>
          val totals = tot.map(t => BikeTotal(t._1, Distance.meter(t._2.asDouble)))
          svc.copy(events = sc, totals = totals)
        }
      })
    }

  def storeAsMaintenance(bs: BikeService): F[MaintenanceId] =
    session.use(s =>
      s.transaction
        .use { _ =>
          for {
            id <- s.unique(MaintenanceSql.insertNoTotals)(bs)
            totals = bs.totals.map(id -> _)
            _ <-
              if (totals.isEmpty) Completion.Update(0).pure[F]
              else s.execute(MaintenanceSql.insertDeviceTotal(totals))(totals)
            _ <- s.execute(MaintenanceSql.clearCacheAfter)(bs.date)
          } yield id
        }
    )

  def storeEvents(mid: MaintenanceId, events: List[TranslatedEvent]): F[Unit] =
    session.use { s =>
      s.transaction.use { _ =>
        events.zipWithIndex.traverse_ { case (te, idx) =>
          for {
            sid <- s.unique(MaintenanceSql.insertServiceEvent)(
              (mid, idx, te.serviceEvent)
            )
            _ <- te.maintEvents.traverse_(e =>
              s.execute(MaintenanceSql.insertEvent)(sid -> e)
            )
          } yield ()
        }
      }
    }

  private def allMaintenances: Stream[F, Maintenance] =
    Stream
      .resource(session)
      .evalMap(_.prepare(MaintenanceSql.allMaintenanceWithTotals))
      .flatMap(_.stream(Void, 100))
      .through(groupEvents)

  private def allMaintenancesUntil(at: Instant): Stream[F, Maintenance] =
    Stream
      .resource(session)
      .evalMap(_.prepare(MaintenanceSql.maintenanceWithTotalsUpTo))
      .flatMap(_.stream(at, 100))
      .through(groupEvents)

  def maintenanceFromLatestCached: F[(MaintenanceBuild, Stream[F, Maintenance])] =
    findLatestCachedBuild(None).flatMap {
      case None =>
        maintenanceZero.map(b => (b, allMaintenances))

      case Some(start) =>
        val stream = Stream
          .resource(session)
          .evalMap(_.prepare(MaintenanceSql.maintenanceWithTotalsAfter))
          .flatMap(_.stream(start.maintenance.id, 100))
          .through(groupEvents)
        (start, stream).pure[F]
    }

  def maintenanceZero: F[MaintenanceBuild] =
    initialTotals.map(t => MaintenanceBuild.empty(MaintenanceId(-1), t))

  private def initialTotals: F[Map[ComponentId, TotalOutput]] = {
    val f = ComponentSql.findInitialTotals(Nil)
    val q = f.fragment.query(Codecs.componentId ~ Codecs.totalOutput)
    session.use(s => s.execute(q)(f.argument)).map(_.toMap)
  }

  def maintenanceFromLatestCachedUntil(
      at: Instant
  ): F[(MaintenanceBuild, Stream[F, Maintenance])] =
    findLatestCachedBuild(Some(at)).flatMap {
      case None =>
        maintenanceZero.map(b => (b, allMaintenancesUntil(at)))

      case Some(start) =>
        val stream = Stream
          .resource(session)
          .evalMap(_.prepare(MaintenanceSql.maintenanceWithTotalsBetween))
          .flatMap(_.stream(at -> start.maintenance.id, 100))
          .through(groupEvents)
        (start, stream).pure[F]
    }

  private def groupEvents(
      in: Stream[F, (MaintenanceId, Option[DeviceId], Option[TotalOutput])]
  ) =
    in.groupAdjacentBy(_._1)
      .evalMap { case (mid, chunk) =>
        loadMaintenance(mid, totalsFrom(chunk.toList))
      }

  private def findLatestCachedBuild(
      before: Option[Instant]
  ): F[Option[MaintenanceBuild]] =
    session.use { s =>
      before
        .map(s.option(ConfigurationCache.findLatestBefore))
        .getOrElse(s.option(ConfigurationCache.findLatest))
        .flatMap {
          case Some(entry) =>
            loadMaintenance(s, entry.maintenanceId)
              .map(m => Some(MaintenanceBuild(m, entry.configuration, entry.tracker)))
          case None =>
            logger
              .warn(s"No cached build entry found given instant: $before")
              .as(Option.empty)
        }
    }

  private def loadMaintenance(s: Session[F], mid: MaintenanceId): F[Maintenance] =
    for {
      events <- s.execute(MaintenanceSql.maintenanceEventsFor)(mid)
      totals <- s.execute(MaintenanceSql.selectTotals)(mid)
    } yield Maintenance(mid, DeviceTotals(totals: _*), events.flatMap(_.toConfigEvent))

  private def loadMaintenance(mid: MaintenanceId, totals: DeviceTotals): F[Maintenance] =
    session
      .use(_.execute(MaintenanceSql.maintenanceEventsFor)(mid))
      .map(events => Maintenance(mid, totals, events.flatMap(_.toConfigEvent)))

  private def totalsFrom(
      chunk: List[(MaintenanceId, Option[DeviceId], Option[TotalOutput])]
  ) =
    DeviceTotals(
      chunk.flatMap(t => t._2.flatMap(id => t._3.map(t => id -> t))): _*
    )

  def generateMissingCache: F[Unit] =
    maintenanceFromLatestCached
      .flatMap { (start, maintenances) =>
        maintenances
          .through(ConfigBuilder.build(start))
          .drop(1) // drops the starting cache entry
          .evalMap { build =>
            session.use(s => s.execute(ConfigurationCache.insert)(build))
          }
          .compile
          .drain
      }
}
