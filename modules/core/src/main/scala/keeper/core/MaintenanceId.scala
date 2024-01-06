package keeper.core

import cats.Eq

import io.bullet.borer.{Decoder, Encoder}

opaque type MaintenanceId = Long

object MaintenanceId:
  def apply(n: Long): MaintenanceId = n

  given Encoder[MaintenanceId] = Encoder.forLong
  given Decoder[MaintenanceId] = Decoder.forLong

  given Eq[MaintenanceId] = Eq.instance((a, b) => a == b)

  extension (self: MaintenanceId) def asLong: Long = self
