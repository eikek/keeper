package keeper.webview.client.components

import java.time.{LocalDate, ZoneId}

import cats.data.ValidatedNel
import cats.syntax.all.*

import keeper.bikes.data.*
import keeper.common.{Distance, Lenses}
import keeper.core.{ComponentId, TotalOutput}
import keeper.webview.client.shared.QuerySelect

import monocle.Lens

final case class FormModel(
    componentId: Option[ComponentId] = None,
    product: QuerySelect.Model[ProductWithBrand] = QuerySelect.Model.empty,
    name: Option[String] = None,
    description: Option[String] = None,
    state: ComponentState = ComponentState.Active,
    addedAt: Option[String] = None,
    initialTotal: Option[String] = None,
    saveInProgress: Boolean = false
):

  def addedAtValidated: ValidatedNel[String, LocalDate] =
    addedAt.map(str => Either.catchNonFatal(LocalDate.parse(str))) match
      case None => "Please set the date the component got to you".invalidNel
      case Some(Left(ex)) =>
        s"Invalid date: ${addedAt.getOrElse("")} (${ex.getMessage})".invalidNel
      case Some(Right(v)) => v.validNel

  def addedAtError: Option[String] =
    addedAtValidated.fold(_.head.some, _ => None)

  def nameValidated: ValidatedNel[String, String] =
    name.toValidNel("A unique name is required")

  def nameError: Option[String] = nameValidated.fold(_.head.some, _ => None)

  def productValidated: ValidatedNel[String, ProductWithBrand] =
    product.value.toValidNel("Please select an appropriate product from the catalogue.")

  def initialTotalValidated: ValidatedNel[String, Distance] =
    initialTotal match
      case Some(t) => Distance.fromString(t).toValidatedNel
      case None    => Distance.zero.validNel

  def initialTotalError: Option[String] =
    initialTotalValidated.fold(_.head.some, _ => None)

  def makeComponent(
      zone: ZoneId
  ): ValidatedNel[String, (Option[ComponentId], NewComponent)] =
    (
      productValidated.map(_.product.id),
      nameValidated,
      description.validNel[String],
      state.validNel[String],
      addedAtValidated.map(_.atTime(12, 0, 0).atZone(zone).toInstant),
      initialTotalValidated.map(TotalOutput.fromDistance)
    ).mapN(NewComponent.apply).map(componentId -> _)

  def setComponent(zone: ZoneId)(c: ComponentWithProduct): FormModel =
    copy(
      componentId = c.component.id.some,
      product = product.setValue(c.productBrand.some, _.show),
      name = c.component.name.some,
      description = c.component.description,
      state = c.component.state,
      addedAt = c.component.addedAt.atZone(zone).toLocalDate.toString.some,
      initialTotal = Option(c.component.initialTotal)
        .filter(_.isPositive)
        .map(_.toDistance)
        .map(_.show),
      saveInProgress = false
    )

object FormModel:

  val productSelect: Lens[FormModel, QuerySelect.Model[ProductWithBrand]] =
    Lens[FormModel, QuerySelect.Model[ProductWithBrand]](_.product)(a =>
      _.copy(product = a)
    )

  val name: Lens[FormModel, String] =
    Lens[FormModel, Option[String]](_.name)(a => _.copy(name = a))
      .andThen(Lenses.emptyString)

  val description: Lens[FormModel, String] =
    Lens[FormModel, Option[String]](_.description)(a => _.copy(description = a))
      .andThen(Lenses.emptyString)

  val state: Lens[FormModel, ComponentState] =
    Lens[FormModel, ComponentState](_.state)(a => _.copy(state = a))

  val initialTotal: Lens[FormModel, String] =
    Lens[FormModel, Option[String]](_.initialTotal)(a => _.copy(initialTotal = a))
      .andThen(Lenses.emptyString)

  val addedAt: Lens[FormModel, String] =
    Lens[FormModel, Option[String]](_.addedAt)(a => _.copy(addedAt = a))
      .andThen(Lenses.emptyString)

  val saveInProgress: Lens[FormModel, Boolean] =
    Lens[FormModel, Boolean](_.saveInProgress)(a => _.copy(saveInProgress = a))

  val saveTrue = saveInProgress.replace(true)
  val saveFalse = saveInProgress.replace(false)
