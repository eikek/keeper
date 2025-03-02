package keeper.bikes.model

import java.time.Instant

import keeper.bikes.data.*
import keeper.common.borer.BaseCodec.given
import keeper.core.*

import io.bullet.borer.NullOptions.given
import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.{Decoder, Encoder}
import monocle.Lens

final case class BrakeCaliper(
    id: ComponentId,
    product: ProductWithBrand,
    name: String,
    description: Option[String],
    state: ComponentState,
    addedAt: Instant,
    initialTotal: TotalOutput,
    createdAt: Instant,
    pad: Option[BasicComponent]
) extends BikePart {
  override def subParts: Set[BikePart] = Set(pad).flatten
}

object BrakeCaliper:
  given Encoder[BrakeCaliper] = deriveEncoder
  given Decoder[BrakeCaliper] = deriveDecoder

  private[model] def fromComponent(
      p: ComponentWithProduct,
      pad: Option[BasicComponent] = None
  ) =
    BrakeCaliper(
      p.id,
      p.productBrand,
      p.component.name,
      p.component.description,
      p.component.state,
      p.component.addedAt,
      p.component.initialTotal,
      p.component.createdAt,
      pad
    )

  val pad: Lens[BrakeCaliper, Option[BasicComponent]] =
    Lens[BrakeCaliper, Option[BasicComponent]](_.pad)(a => _.copy(pad = a))
