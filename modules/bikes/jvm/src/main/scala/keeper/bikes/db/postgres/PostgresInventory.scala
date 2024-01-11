package keeper.bikes.db.postgres

import java.time.Instant

import cats.NonEmptyParallel
import cats.data.{EitherT, NonEmptyList, OptionT}
import cats.effect.*
import cats.effect.std.Console
import cats.syntax.all.*
import fs2.io.net.Network

import keeper.bikes.data.*
import keeper.bikes.db.*
import keeper.bikes.db.migration.SchemaMigration
import keeper.bikes.model.*
import keeper.common.Distance
import keeper.core.*

import org.typelevel.otel4s.trace.Tracer
import skunk.Session

final class PostgresInventory[F[_]: Sync: NonEmptyParallel](
    session: Resource[F, Session[F]]
) extends Inventory[F] {
  private[this] val logger = scribe.cats.effect[F]

  val brands: BrandRepository[F] = new PostgresBrandRepository[F](session)
  val products: ProductRepository[F] = new PostgresProductRepository[F](session)
  val components: ComponentRepository[F] = new PostgresComponentRepo[F](session)
  val devices: DeviceRepository[F] = new PostgresDeviceRepo[F](session)
  val maintenance: MaintenanceRepository[F] = new PostgresMaintenanceRepo[F](session)

  val componentSource: ComponentSource[F] = new ComponentSource[F]:
    def findDevice(id: DeviceId, at: Instant): F[Option[DeviceWithBrand]] =
      devices.findByIdWithBrand(id, at)
    def findComponent(id: ComponentId, at: Instant): F[Option[ComponentWithProduct]] =
      components.findByIdWithProduct(id, at)
    def getComponentsOfType(at: Instant, cts: NonEmptyList[ComponentType]) =
      components.findAllByType(cts, at).map(_.toSeq)

  val resolver = new BikesResolve[F](componentSource)

  def getCurrentBikes(
      currentTotals: List[BikeTotal]
  ): F[Either[BikesResolveError, BikeBuilds]] =
    Clock[F].realTimeInstant.flatMap(at => getBikesAt(at, currentTotals))

  def getBuildsAt(at: Instant): F[MaintenanceBuild] =
    logger.info(s"Getting builds at $at") >>
      maintenance.maintenanceFromLatestCachedUntil(at).flatMap { (start, maintenances) =>
        OptionT(
          maintenances
            .through(ConfigBuilder.build(start))
            .compile
            .last
        ).getOrElseF(maintenance.maintenanceZero(at))
      }

  def getBikesAt(
      at: Instant,
      currentTotals: List[BikeTotal]
  ): F[Either[BikesResolveError, BikeBuilds]] =
    val devTotals = DeviceTotals(
      currentTotals.map(b => b.bikeId -> TotalOutput(b.distance.toMeter)).toMap
    )
    (for {
      mb <- EitherT.liftF(getBuildsAt(at))
      resolved <- EitherT(resolver.resolve(mb.build, at))
      totals = mb.tracker
        .finish(devTotals)
        .view
        .mapValues(_.toDistance)
        .toMap

      modify = BikeBuilds
        .addComponentTotals(totals)
        .andThen(BikeBuilds.bikeTotals.replace(currentTotals))
    } yield modify(resolved)).value

  def getComponentsAt(at: Instant): F[List[ComponentWithDevice]] =
    (components.findAllAt(at), getBuildsAt(at))
      .parMapN { (comps, build) =>
        comps.map(c => c -> build.build.findDevice(c.id))
      }
      .flatMap(_.traverse { case (c, optDevice) =>
        val dev = optDevice.flatTraverse(devices.findByIdWithBrand(_, at))
        dev.map(d => ComponentWithDevice(c, d))
      })
}

object PostgresInventory:
  def resource[F[_]: Tracer: Network: Console: Temporal: Sync: NonEmptyParallel](
      cfg: PostgresConfig
  ): Resource[F, PostgresInventory[F]] =
    SkunkSession[F](cfg)
      .evalTap(r => r.use(s => SchemaMigration[F](s).migrate)(Sync[F]))
      .map(new PostgresInventory[F](_))
