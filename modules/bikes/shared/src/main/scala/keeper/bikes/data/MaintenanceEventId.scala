package keeper.bikes.data

import io.bullet.borer.{Decoder, Encoder}

opaque type MaintenanceEventId = Long

object MaintenanceEventId:
  def apply(n: Long): MaintenanceEventId = n

  given Encoder[MaintenanceEventId] = Encoder.forLong
  given Decoder[MaintenanceEventId] = Decoder.forLong.map(MaintenanceEventId.apply)

  extension (self: MaintenanceEventId) def asLong: Long = self
