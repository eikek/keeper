package keeper.bikes.data

import io.bullet.borer.{Decoder, Encoder}

opaque type ActionId = Long

object ActionId:
  def apply(n: Long): ActionId = n

  given Encoder[ActionId] = Encoder.forLong
  given Decoder[ActionId] = Decoder.forLong

  extension (self: ActionId) def asLong: Long = self
