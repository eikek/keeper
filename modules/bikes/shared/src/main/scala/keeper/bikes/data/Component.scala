package keeper.bikes.data

import java.time.Instant
import keeper.common.borer.BaseCodec.given
import keeper.core.{ComponentId, TotalOutput}
import io.bullet.borer.NullOptions.*
import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.{Decoder, Encoder}
import keeper.common.Distance

final case class Component(
    id: ComponentId,
    product: ProductId,
    name: String,
    description: Option[String],
    state: ComponentState,
    addedAt: Instant,
    removedAt: Option[Instant],
    initialTotal: TotalOutput,
    createdAt: Instant
):

  lazy val initialDistance: Distance = initialTotal.toDistance

object Component:
  given Encoder[Component] = deriveEncoder
  given Decoder[Component] = deriveDecoder
