package keeper.bikes.data

import cats.Show
import cats.kernel.Eq

import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.{Decoder, Encoder}

final case class ProductWithBrand(
    product: BikeProduct,
    brand: Brand
)

object ProductWithBrand:
  given Encoder[ProductWithBrand] = deriveEncoder
  given Decoder[ProductWithBrand] = deriveDecoder

  given Eq[ProductWithBrand] = Eq.fromUniversalEquals

  given Show[ProductWithBrand] =
    Show.show(p => s"${p.product.productType.name}, ${p.brand.name} ${p.product.name}")
