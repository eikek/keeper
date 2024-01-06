package keeper.bikes.data

import cats.{Eq, Show}

import keeper.core.ComponentId

import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.{Decoder, Encoder}

final case class ComponentWithProduct(
    product: BikeProduct,
    brand: Brand,
    component: Component
):
  val id: ComponentId = component.id

  val productBrand: ProductWithBrand =
    ProductWithBrand(product, brand)

object ComponentWithProduct:
  given Encoder[ComponentWithProduct] = deriveEncoder

  given Decoder[ComponentWithProduct] = deriveDecoder

  given Eq[ComponentWithProduct] = Eq.fromUniversalEquals

  given Show[ComponentWithProduct] =
    Show.show(p =>
      s"${p.product.productType.name}, ${p.component.name} (${p.product.name})"
    )
