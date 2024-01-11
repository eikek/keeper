package keeper.bikes.model

import java.time.Instant
import cats.Monad
import cats.data.{EitherT, NonEmptyList}
import cats.syntax.all.*
import keeper.bikes.data.{ComponentWithProduct, DeviceWithBrand}
import keeper.common.Distance
import keeper.core.{ComponentId, DeviceBuild, DeviceId}

final private case class ResolvedBuild(
    devices: Map[DeviceId, DeviceWithBrand],
    topLevel: Map[DeviceId, List[ComponentWithProduct]],
    subComponents: Map[ComponentId, List[ComponentWithProduct]]
):
  def getTopLevel(id: DeviceId): List[ComponentWithProduct] = topLevel.getOrElse(id, Nil)
  def getSubComponents(id: ComponentId): List[ComponentWithProduct] =
    subComponents.getOrElse(id, Nil)

  def getBikes: List[Bike] = devices.values.map(toBike).toList

  def getBike(id: DeviceId): Option[Bike] =
    devices.get(id).map(toBike)

  def initialTotals: Map[ComponentId, Distance] = {
    val x = topLevel.values.flatMap(_.map(c => c.id -> c.component.initialDistance))
    val y = subComponents.values.flatMap(_.map(c => c.id -> c.component.initialDistance))

    (x ++ y).filter(_._2 > Distance.zero).toMap
  }

  private def toBike(e: DeviceWithBrand): Bike =
    Bike(
      e.device.id,
      e.brand,
      e.device.name,
      e.device.description,
      e.device.state,
      e.device.addedAt,
      e.device.createdAt
    )

object ResolvedBuild:
  def load[F[_]: Monad](
      source: ComponentSource[F],
      build: DeviceBuild,
      at: Instant
  ): EitherT[F, BikesResolveError, ResolvedBuild] =
    for {
      devices <- build.devices.keySet.toList
        .traverse(devId =>
          EitherT
            .fromOptionF(source.findDevice(devId, at), NonEmptyList.of(devId))
            .map(devId -> _)
        )
        .map(_.toMap)
        .leftMap(BikesResolveError.BikeNotFound.apply)

      topLevel <-
        build.devices.toList
          .traverse { case (dev, comps) =>
            comps.toList
              .traverse(cid =>
                EitherT.fromOptionF(source.findComponent(cid, at), NonEmptyList.of(cid))
              )
              .map(rc => dev -> rc)
          }
          .map(_.toMap)
          .leftMap(BikesResolveError.ComponentNotFound.apply)

      comps <- build.components.toList
        .traverse { case (p, c) =>
          c.toList
            .traverse(id =>
              EitherT.fromOptionF(source.findComponent(id, at), NonEmptyList.of(id))
            )
            .map(rc => p -> rc)
        }
        .map(_.toMap)
        .leftMap(BikesResolveError.ComponentNotFound.apply)
    } yield ResolvedBuild(devices, topLevel, comps)
