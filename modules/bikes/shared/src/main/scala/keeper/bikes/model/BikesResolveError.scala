package keeper.bikes.model

import cats.data.NonEmptyList
import cats.syntax.all.*

import keeper.core.{ComponentId, DeviceId}

import io.bullet.borer.Encoder

sealed abstract class BikesResolveError(msg: String) extends RuntimeException(msg)

object BikesResolveError:
  final case class BikeNotFound(ids: NonEmptyList[DeviceId])
      extends BikesResolveError(s"Bikes not found: $ids")

  final case class ComponentNotFound(ids: NonEmptyList[ComponentId])
      extends BikesResolveError(s"Component not found: $ids")

  given Encoder[BikesResolveError] =
    Encoder.of[Map[String, String]].contramap(err => Map("message" -> err.getMessage))
