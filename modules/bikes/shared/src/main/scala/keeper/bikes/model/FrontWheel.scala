package keeper.bikes.model

import java.time.Instant

import cats.kernel.Eq

import keeper.bikes.data.*
import keeper.common.borer.BaseCodec.given
import keeper.core.*

import io.bullet.borer.NullOptions.given
import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.{Decoder, Encoder}
import monocle.Lens

final case class FrontWheel(
    id: ComponentId,
    product: ProductWithBrand,
    name: String,
    description: Option[String],
    state: ComponentState,
    addedAt: Instant,
    initialTotal: TotalOutput,
    createdAt: Instant,
    brakeDisc: Option[BasicComponent],
    tire: Option[BasicComponent],
    innerTube: Option[BasicComponent]
) extends BikePart {
  override def subParts: Set[BikePart] = Set(brakeDisc, tire, innerTube).flatten
}

object FrontWheel:
  given Encoder[FrontWheel] = deriveEncoder
  given Decoder[FrontWheel] = deriveDecoder
  given Eq[FrontWheel] = Eq.fromUniversalEquals

  private[model] def fromComponent(
      p: ComponentWithProduct,
      brakeDisc: Option[BasicComponent] = None,
      tire: Option[BasicComponent] = None,
      innerTube: Option[BasicComponent] = None
  ) =
    FrontWheel(
      p.component.id,
      p.productBrand,
      p.component.name,
      p.component.description,
      p.component.state,
      p.component.addedAt,
      p.component.initialTotal,
      p.component.createdAt,
      brakeDisc,
      tire,
      innerTube
    )

  val brakeDisc: Lens[FrontWheel, Option[BasicComponent]] =
    Lens[FrontWheel, Option[BasicComponent]](_.brakeDisc)(a => _.copy(brakeDisc = a))

  val tire: Lens[FrontWheel, Option[BasicComponent]] =
    Lens[FrontWheel, Option[BasicComponent]](_.tire)(a => _.copy(tire = a))

  val tube: Lens[FrontWheel, Option[BasicComponent]] =
    Lens[FrontWheel, Option[BasicComponent]](_.innerTube)(a => _.copy(innerTube = a))
