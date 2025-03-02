package keeper.bikes.data

import java.time.Instant

import keeper.common.borer.BaseCodec.given
import keeper.core.*

import io.bullet.borer.NullOptions.given
import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.{Decoder, Encoder}

case class NewComponent(
    product: ProductId,
    name: String,
    description: Option[String],
    state: ComponentState,
    addedAt: Instant,
    initialTotal: TotalOutput
)

object NewComponent:
  given Decoder[NewComponent] = deriveDecoder
  given Encoder[NewComponent] = deriveEncoder
