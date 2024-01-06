package keeper.webview.client.newservice

import java.time.{LocalDateTime, ZoneId}

import cats.data.ValidatedNel
import cats.syntax.all.*

import keeper.bikes.model.BikeService
import keeper.common.Lenses
import keeper.webview.client.util.DateTime

import monocle.Lens

final case class MetadataModel(
    name: String = "",
    description: Option[String] = None,
    date: String = "",
    time: String = ""
):
  def nameValidated: ValidatedNel[String, String] =
    if (name.isEmpty) "A name is required".invalidNel else name.validNel

  def dateTimeValidated: ValidatedNel[String, LocalDateTime] =
    DateTime.parseDateTime(date, time).toValidatedNel

  def isValid =
    nameValidated.isValid && dateTimeValidated.isValid

  def isInvalid = !isValid

  def asBikeService(zoneId: ZoneId): ValidatedNel[String, BikeService] =
    (dateTimeValidated, nameValidated).mapN { (dt, name) =>
      BikeService(
        name = name,
        description = description,
        date = dt.atZone(zoneId).toInstant,
        createdAt = None,
        totals = Nil,
        events = Nil
      )
    }

object MetadataModel:
  val name: Lens[MetadataModel, String] =
    Lens[MetadataModel, String](_.name)(a => _.copy(name = a))

  val description: Lens[MetadataModel, String] =
    Lens[MetadataModel, Option[String]](_.description)(a => _.copy(description = a))
      .andThen(Lenses.emptyString)

  val date: Lens[MetadataModel, String] =
    Lens[MetadataModel, String](_.date)(a => _.copy(date = a))

  val time: Lens[MetadataModel, String] =
    Lens[MetadataModel, String](_.time)(a => _.copy(time = a))
