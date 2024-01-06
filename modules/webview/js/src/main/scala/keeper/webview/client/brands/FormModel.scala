package keeper.webview.client.brands

import cats.data.ValidatedNel
import cats.syntax.all.*

import keeper.bikes.data.*
import keeper.common.Lenses

import monocle.Lens

final case class FormModel(
    brandId: Option[BrandId] = None,
    name: Option[String] = None,
    description: Option[String] = None,
    saveInProgress: Boolean = false
):

  def nameValidated: ValidatedNel[String, String] =
    name.toValidNel("A uniqe name is required")

  def nameError: Option[String] = nameValidated.fold(_.head.some, _ => None)

  def makeBrand: ValidatedNel[String, (Option[BrandId], NewBrand)] =
    (
      nameValidated,
      description.validNel[String]
    ).mapN(NewBrand.apply).map(brandId -> _)

  def setBrand(c: Brand): FormModel =
    copy(
      brandId = c.id.some,
      name = c.name.some,
      description = c.description,
      saveInProgress = false
    )

object FormModel:
  val name: Lens[FormModel, String] =
    Lens[FormModel, Option[String]](_.name)(a => _.copy(name = a))
      .andThen(Lenses.emptyString)

  val description: Lens[FormModel, String] =
    Lens[FormModel, Option[String]](_.description)(a => _.copy(description = a))
      .andThen(Lenses.emptyString)

  val saveInProgress: Lens[FormModel, Boolean] =
    Lens[FormModel, Boolean](_.saveInProgress)(a => _.copy(saveInProgress = a))

  val saveTrue = saveInProgress.replace(true)
  val saveFalse = saveInProgress.replace(false)
