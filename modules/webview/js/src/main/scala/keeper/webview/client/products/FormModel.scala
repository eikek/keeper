package keeper.webview.client.products

import cats.data.{Validated, ValidatedNel}
import cats.syntax.all.*

import keeper.bikes.data.*
import keeper.common.Lenses
import keeper.webview.client.shared.QuerySelect

import monocle.Lens

final case class FormModel(
    productId: Option[ProductId],
    productTypeSelect: QuerySelect.Model[ComponentType],
    brandSelectModel: QuerySelect.Model[Brand],
    name: Option[String],
    weight: Option[String],
    description: Option[String],
    saveInProgress: Boolean
):
  def setProduct(p: ProductWithBrand): FormModel =
    copy(
      productId = p.product.id.some,
      productTypeSelect =
        productTypeSelect.setValue(p.product.productType.some, _.toString),
      brandSelectModel = brandSelectModel.setValue(p.brand.some, _.name),
      name = p.product.name.some,
      weight = p.product.weight.map(_.toGramm.toString),
      description = p.product.description
    )

  def brandValidated: ValidatedNel[String, Brand] =
    brandSelectModel.value.toValidNel("A bike brand is required.")

  def nameValidated: ValidatedNel[String, String] =
    name.toValidNel("A name is required.")

  def nameError: Option[String] =
    nameValidated.fold(_.head.some, _ => None)

  def weightValidated: ValidatedNel[String, Option[Weight]] =
    weight.traverse(Weight.fromString(5000))

  def weightError: Option[String] = weightValidated.fold(_.head.some, _ => None)

  def productTypeValidated: ValidatedNel[String, ComponentType] =
    productTypeSelect.value.toValidNel("A product type is required.")

  def makeProduct: ValidatedNel[String, (Option[ProductId], NewBikeProduct)] =
    (
      brandValidated.map(_.id),
      productTypeValidated,
      nameValidated,
      Validated.validNel(description),
      weightValidated
    )
      .mapN(NewBikeProduct.apply)
      .map(p => (productId, p))

object FormModel:
  val empty: FormModel =
    FormModel(
      None,
      QuerySelect.Model.empty[ComponentType],
      QuerySelect.Model.empty[Brand],
      None,
      None,
      None,
      false
    )

  val productTypeSelect: Lens[FormModel, QuerySelect.Model[ComponentType]] =
    Lens[FormModel, QuerySelect.Model[ComponentType]](_.productTypeSelect)(a =>
      _.copy(productTypeSelect = a)
    )

  val brandSelect: Lens[FormModel, QuerySelect.Model[Brand]] =
    Lens[FormModel, QuerySelect.Model[Brand]](_.brandSelectModel)(a =>
      _.copy(brandSelectModel = a)
    )

  val name: Lens[FormModel, String] =
    Lens[FormModel, Option[String]](_.name)(a => _.copy(name = a))
      .andThen(Lenses.emptyString)

  val description: Lens[FormModel, String] =
    Lens[FormModel, Option[String]](_.description)(a => _.copy(description = a))
      .andThen(Lenses.emptyString)

  val weight: Lens[FormModel, String] =
    Lens[FormModel, Option[String]](_.weight)(a => _.copy(weight = a))
      .andThen(Lenses.emptyString)

  val saveInProgress: Lens[FormModel, Boolean] =
    Lens[FormModel, Boolean](_.saveInProgress)(a => _.copy(saveInProgress = a))

  val saveTrue = saveInProgress.replace(true)
  val saveFalse = saveInProgress.replace(false)
