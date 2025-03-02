package keeper.bikes.data

import java.time.Instant

import keeper.common.borer.BaseCodec.given

import io.bullet.borer.NullOptions.given
import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.{Decoder, Encoder}

final case class BikeProduct(
    id: ProductId,
    brandId: BrandId,
    productType: ComponentType,
    name: String,
    description: Option[String],
    weight: Option[Weight],
    createdAt: Instant
):
  def isType(ct: ComponentType): Boolean = productType == ct

object BikeProduct:
  given Encoder[BikeProduct] = deriveEncoder
  given Decoder[BikeProduct] = deriveDecoder
