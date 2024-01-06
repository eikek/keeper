package keeper.webview.client.newbike

import java.time.{LocalDate, LocalDateTime, LocalTime}

import cats.data.ValidatedNel
import cats.syntax.all.*

import keeper.bikes.data.Brand
import keeper.common.Lenses
import keeper.core.TotalOutput
import keeper.webview.client.shared.QuerySelect

import monocle.Lens

final case class MetadataModel(
    brandSelectModel: QuerySelect.Model[Brand] = QuerySelect.Model.empty[Brand],
    name: String = "",
    description: Option[String] = None,
    date: String = "",
    time: String = "",
    initialDistance: Option[String] = Some("0")
):
  def brandValidated: ValidatedNel[String, Brand] =
    brandSelectModel.value.toValidNel("A bike brand is required.")

  def nameValidated: ValidatedNel[String, String] =
    if (name.isEmpty) "A name is required".invalidNel else name.validNel

  def dateTimeValidated: ValidatedNel[String, LocalDateTime] =
    (
      Either.catchNonFatal(LocalDate.parse(date)).left.map(_.getMessage).toValidatedNel,
      Either.catchNonFatal(LocalTime.parse(time)).left.map(_.getMessage).toValidatedNel
    ).mapN((d, t) => LocalDateTime.of(d, t))

  def initialDistanceValidated: ValidatedNel[String, TotalOutput] =
    initialDistance match
      case Some(t) => TotalOutput.fromString(t).toValidatedNel[String]
      case None    => TotalOutput.zero.validNel

  def isValid =
    brandValidated.isValid && nameValidated.isValid && dateTimeValidated.isValid &&
      initialDistanceValidated.isValid

  def isInvalid = !isValid

object MetadataModel:
  val brandSelect: Lens[MetadataModel, QuerySelect.Model[Brand]] =
    Lens[MetadataModel, QuerySelect.Model[Brand]](_.brandSelectModel)(a =>
      _.copy(brandSelectModel = a)
    )

  val name: Lens[MetadataModel, String] =
    Lens[MetadataModel, String](_.name)(a => _.copy(name = a))

  val description: Lens[MetadataModel, String] =
    Lens[MetadataModel, Option[String]](_.description)(a => _.copy(description = a))
      .andThen(Lenses.emptyString)

  val date: Lens[MetadataModel, String] =
    Lens[MetadataModel, String](_.date)(a => _.copy(date = a))

  val time: Lens[MetadataModel, String] =
    Lens[MetadataModel, String](_.time)(a => _.copy(time = a))

  val initialDistance: Lens[MetadataModel, String] =
    Lens[MetadataModel, Option[String]](_.initialDistance)(a =>
      _.copy(initialDistance = a)
    )
      .andThen(Lenses.emptyString)
