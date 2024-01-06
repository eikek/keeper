package keeper.bikes.service

import java.time.Instant

import scala.collection.immutable.SortedSet

import cats.Id
import cats.data.{EitherT, NonEmptySet}
import cats.effect.Sync
import cats.syntax.all.*
import fs2.Stream

import keeper.bikes.BikeServiceProvider
import keeper.bikes.data._
import keeper.bikes.db.{Inventory, MaintenanceRepository}
import keeper.bikes.event.ServiceEvent
import keeper.bikes.model.*
import keeper.core.{DeviceBuild, TypeInfo}

final class DefaultServiceProvider[F[_]: Sync](
    inventory: Inventory[F],
    repo: MaintenanceRepository[F]
) extends BikeServiceProvider[F]
    with NewBikeResolve[F] {

  private def resolveBuild(
      build: DeviceBuild,
      at: Instant,
      nextSource: ComponentSource[F]
  ): EitherT[F, BikeServiceError, BikeBuilds] =
    val resolve = new BikesResolve[F](inventory.componentSource.andThen(nextSource))
    resolve.resolveT(build, at).leftMap(re => BikeServiceError.ComponentResolveError(re))

  def previewBikeService(
      date: Instant,
      serviceEvents: List[ServiceEvent]
  ): F[Either[BikeServiceError, BikeBuilds]] =
    (for {
      current <- EitherT.right(inventory.getBuildsAt(date))
      res <- validateEvents(current.build, date, serviceEvents)
    } yield res).value

  def processBikeService(
      service: BikeService
  ): F[Either[BikeServiceError, BikeBuilds]] =
    inventory.getBuildsAt(service.date).flatMap { current =>
      // first build things in memory and check if resulting build is good
      validateEvents(current.build, service.date, service.events).flatMap { _ =>
        for {
          // preprocess events to create new devices and inject proper id
          events <- EitherT.right(
            Stream
              .emits(service.events)
              .covary[F]
              .through(resolveNewBikes(inventory.devices))
              .compile
              .toList
          )

          // create new maintenance row
          id <- EitherT.right(repo.storeAsMaintenance(service))

          result <- validateApply(current.build, events)

          _ <- EitherT.right(
            repo.storeEvents(
              id,
              result._2.map(_.updateEvents(NewMaintenanceEvent.maintenance.replace(id)))
            )
          )

          // also cache the outcome for quicker reading
          // fix eventually removed cache entries
          _ <- EitherT.right(repo.generateMissingCache)

          // remove ceased components and devices
          _ <- EitherT.right(ceaseComponents(result._2, service))
          _ <- EitherT.right(ceaseBikes(result._2, service))

          out <- resolveBuild(result._1, service.date, ComponentSource.empty[F])
        } yield out
      }.value
    }

  private def ceaseComponents(events: List[TranslatedEvent], service: BikeService) = {
    val ids =
      events
        .flatMap(_.maintEvents.filter(_.action == ActionName.Cease))
        .flatMap(e => Set(e.component, e.subComponent).flatten)

    NonEmptySet.fromSet(SortedSet.from(ids)) match
      case Some(nes) =>
        inventory.components.removeComponents(nes, service.date)
      case None => ().pure[F]
  }

  private def ceaseBikes(events: List[TranslatedEvent], service: BikeService) = {
    val ids =
      events
        .flatMap(
          _.maintEvents.filter(e =>
            e.action == ActionName.Cease && e.component.isEmpty && e.subComponent.isEmpty
          )
        )
        .flatMap(_.device)

    NonEmptySet.fromSet(SortedSet.from(ids)) match
      case None      => ().pure[F]
      case Some(ids) => inventory.devices.removeDevices(ids, service.date)
  }

  private def validateEvents(
      current: DeviceBuild,
      date: Instant,
      events: List[ServiceEvent]
  ): EitherT[F, BikeServiceError, BikeBuilds] =
    val resolved = resolveNewBikesEphemeral(current, events)
    val evs = resolved.map(_._1)
    val newDevices = resolved.flatMap(_._2)
    for {
      brands <- newDevices
        .map(_.brand.id)
        .traverse { bid =>
          loadBrand(bid).map(bid -> _)
        }
        .map(_.toMap)

      memSource = ComponentSource.from(
        newDevices
          .map(e => e.device.id -> e.copy(brand = brands.getOrElse(e.brand.id, e.brand)))
          .toMap
          .get,
        _ => None,
        _ => Seq.empty
      )
      next <- validateApply(current, evs).map(_._1)
      res <- resolveBuild(next, date, memSource)
    } yield res

  private def validateApply(
      current: DeviceBuild,
      events: List[ServiceEvent]
  ): EitherT[F, BikeServiceError, (DeviceBuild, List[TranslatedEvent])] = EitherT {
    inventory.components.checkTypes(events.flatMap(_.componentTypes).distinct).flatMap {
      case Some(err) => err.asLeft.pure[F]
      case None =>
        val ids = events.flatMap(_.componentTypes.map(_._1)).toSet ++
          DeviceBuild.allComponentIds.apply(current)
        inventory.components
          .queryTypes(ids)
          .map(TypeInfo.fromMap[Id, ComponentType])
          .map(new ServiceEventMapper(_))
          .map(_.validateApply(current, events))
    }
  }

  private def loadBrand(id: BrandId): EitherT[F, BikeServiceError, Brand] =
    EitherT.fromOptionF(inventory.brands.findById(id), BikeServiceError.BrandNotFound(id))
}
