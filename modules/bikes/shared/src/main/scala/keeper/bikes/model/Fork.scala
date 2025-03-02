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

final case class Fork(
    id: ComponentId,
    product: ProductWithBrand,
    name: String,
    description: Option[String],
    state: ComponentState,
    addedAt: Instant,
    initialTotal: TotalOutput,
    createdAt: Instant,
    brakeCaliper: Option[BrakeCaliper],
    mudguard: Option[BasicComponent]
) extends BikePart {
  override def subParts: Set[BikePart] = Set(mudguard, brakeCaliper).flatten
}

object Fork:
  given Encoder[Fork] = deriveEncoder
  given Decoder[Fork] = deriveDecoder
  given Eq[Fork] = Eq.fromUniversalEquals

  private[model] def fromComponent(
      p: ComponentWithProduct,
      brake: Option[BrakeCaliper] = None,
      mudguard: Option[BasicComponent] = None
  ) =
    Fork(
      p.component.id,
      p.productBrand,
      p.component.name,
      p.component.description,
      p.component.state,
      p.component.addedAt,
      p.component.initialTotal,
      p.component.createdAt,
      brake,
      mudguard
    )

  val brakeCaliper: Lens[Fork, Option[BrakeCaliper]] =
    Lens[Fork, Option[BrakeCaliper]](_.brakeCaliper)(a => _.copy(brakeCaliper = a))

  val mudguard: Lens[Fork, Option[BasicComponent]] =
    Lens[Fork, Option[BasicComponent]](_.mudguard)(a => _.copy(mudguard = a))
