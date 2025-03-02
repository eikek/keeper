package keeper.bikes.model

import java.time.Instant

import keeper.bikes.data.*
import keeper.common.borer.BaseCodec.given
import keeper.core.*

import io.bullet.borer.NullOptions.given
import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.{Decoder, Encoder}

final case class BasicComponent(
    id: ComponentId,
    product: ProductWithBrand,
    name: String,
    description: Option[String],
    state: ComponentState,
    addedAt: Instant,
    initialTotal: TotalOutput,
    createdAt: Instant
) extends BikePart:
  override val subParts: Set[BikePart] = Set.empty

object BasicComponent:
  given Decoder[BasicComponent] = deriveDecoder
  given Encoder[BasicComponent] = deriveEncoder

  private[model] def fromComponent(p: ComponentWithProduct) =
    BasicComponent(
      p.component.id,
      p.productBrand,
      p.component.name,
      p.component.description,
      p.component.state,
      p.component.addedAt,
      p.component.initialTotal,
      p.component.createdAt
    )
