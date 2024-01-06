package keeper.bikes.model

import java.time.Instant

import cats.Eq

import keeper.bikes.data.*
import keeper.common.borer.BaseCodec.given
import keeper.core.*

import io.bullet.borer.NullOptions.*
import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.{Decoder, Encoder}
import monocle.Lens

final case class RearWheel(
    id: ComponentId,
    product: ProductWithBrand,
    name: String,
    description: Option[String],
    state: ComponentState,
    addedAt: Instant,
    initialTotal: TotalOutput,
    createdAt: Instant,
    cassette: Option[BasicComponent],
    brakeDisc: Option[BasicComponent],
    tire: Option[BasicComponent],
    innerTube: Option[BasicComponent]
) extends BikePart {
  override def subParts: Set[BikePart] = Set(cassette, brakeDisc, innerTube, tire).flatten
}

object RearWheel:
  given Encoder[RearWheel] = deriveEncoder
  given Decoder[RearWheel] = deriveDecoder
  given Eq[RearWheel] = Eq.fromUniversalEquals

  private[model] def fromComponent(
      p: ComponentWithProduct,
      cassette: Option[BasicComponent] = None,
      brakeDisc: Option[BasicComponent] = None,
      tire: Option[BasicComponent] = None,
      innerTube: Option[BasicComponent] = None
  ) =
    RearWheel(
      p.id,
      p.productBrand,
      p.component.name,
      p.component.description,
      p.component.state,
      p.component.addedAt,
      p.component.initialTotal,
      p.component.createdAt,
      cassette,
      brakeDisc,
      tire,
      innerTube
    )

  val brakeDisc: Lens[RearWheel, Option[BasicComponent]] =
    Lens[RearWheel, Option[BasicComponent]](_.brakeDisc)(a => _.copy(brakeDisc = a))

  val tire: Lens[RearWheel, Option[BasicComponent]] =
    Lens[RearWheel, Option[BasicComponent]](_.tire)(a => _.copy(tire = a))

  val tube: Lens[RearWheel, Option[BasicComponent]] =
    Lens[RearWheel, Option[BasicComponent]](_.innerTube)(a => _.copy(innerTube = a))

  val cassette: Lens[RearWheel, Option[BasicComponent]] =
    Lens[RearWheel, Option[BasicComponent]](_.cassette)(a => _.copy(cassette = a))
