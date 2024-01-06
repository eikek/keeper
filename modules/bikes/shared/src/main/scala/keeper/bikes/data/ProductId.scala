package keeper.bikes.data

import io.bullet.borer.{Decoder, Encoder}

opaque type ProductId = Long

object ProductId:
  def apply(n: Long): ProductId = n

  given Encoder[ProductId] = Encoder.forLong
  given Decoder[ProductId] = Decoder.forLong

  extension (self: ProductId) def asLong: Long = self
