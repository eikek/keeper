package keeper.bikes.data

import io.bullet.borer.{Decoder, Encoder}

opaque type BrandId = Int

object BrandId:
  def apply(n: Int): BrandId = n

  given Encoder[BrandId] = Encoder.forInt
  given Decoder[BrandId] = Decoder.forInt

  extension (self: BrandId) def asInt: Int = self
