package keeper.bikes.data

import cats.Eq

import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.{Decoder, Encoder}

final case class DeviceWithBrand(
    device: Device,
    brand: Brand
)

object DeviceWithBrand:
  given Encoder[DeviceWithBrand] = deriveEncoder
  given Decoder[DeviceWithBrand] = deriveDecoder
  given Eq[DeviceWithBrand] = Eq.fromUniversalEquals
