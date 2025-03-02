package keeper.bikes.data

import io.bullet.borer.NullOptions.given
import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.{Decoder, Encoder}

final case class NewBrand(
    name: String,
    description: Option[String]
)

object NewBrand:
  given Decoder[NewBrand] = deriveDecoder
  given Encoder[NewBrand] = deriveEncoder
