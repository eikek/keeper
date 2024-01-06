package keeper.bikes.data

import keeper.common.Lenses

import io.bullet.borer.NullOptions.*
import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.{Decoder, Encoder}
import monocle.Lens

final case class NewBikeProduct(
    brandId: BrandId,
    productType: ComponentType,
    name: String,
    description: Option[String],
    weight: Option[Weight]
)

object NewBikeProduct:
  given Decoder[NewBikeProduct] = deriveDecoder
  given Encoder[NewBikeProduct] = deriveEncoder

  val brandId: Lens[NewBikeProduct, BrandId] =
    Lens[NewBikeProduct, BrandId](_.brandId)(a => _.copy(brandId = a))

  val productType: Lens[NewBikeProduct, ComponentType] =
    Lens[NewBikeProduct, ComponentType](_.productType)(a => _.copy(productType = a))

  val name: Lens[NewBikeProduct, String] =
    Lens[NewBikeProduct, String](_.name)(a => _.copy(name = a))

  val description: Lens[NewBikeProduct, Option[String]] =
    Lens[NewBikeProduct, Option[String]](_.description)(a => _.copy(description = a))

  val descr: Lens[NewBikeProduct, String] =
    description.andThen(Lenses.emptyString)

  val weight: Lens[NewBikeProduct, Option[Weight]] =
    Lens[NewBikeProduct, Option[Weight]](_.weight)(a => _.copy(weight = a))

  val weightGramm: Lens[NewBikeProduct, String] =
    weight.andThen(
      Lens[Option[Weight], String](_.map(_.toGramm.toInt.toString).getOrElse(""))(str =>
        w => str.toIntOption.map(n => Weight.gramm(n)).orElse(w)
      )
    )
