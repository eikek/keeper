package keeper.bikes.data

import cats.Eq

import keeper.core.ComponentId

import io.bullet.borer.NullOptions.given
import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.{Decoder, Encoder}

final case class ComponentWithDevice(
    component: ComponentWithProduct,
    device: Option[DeviceWithBrand]
):
  val id: ComponentId = component.component.id

  def containsName(str: String) =
    val lcs = str.toLowerCase.split("\\s+").toList
    lcs.forall { lc =>
      device.exists(_.device.name.toLowerCase.contains(lc)) ||
      component.product.name.toLowerCase.contains(lc) ||
      component.product.productType.name.toLowerCase.contains(lc) ||
      component.brand.name.toLowerCase.contains(lc) ||
      component.component.name.toLowerCase.contains(lc)
    }

object ComponentWithDevice:
  given Encoder[ComponentWithDevice] = deriveEncoder
  given Decoder[ComponentWithDevice] = deriveDecoder
  given Eq[ComponentWithDevice] = Eq.fromUniversalEquals
