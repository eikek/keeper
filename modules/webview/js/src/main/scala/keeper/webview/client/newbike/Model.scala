package keeper.webview.client.newbike

import java.time.ZoneId

import cats.Eq
import cats.data.ValidatedNel
import cats.syntax.all.*

import keeper.bikes.event.ServiceEvent
import keeper.bikes.model.BikeService

import monocle.Lens

final case class Model(
    submitInProgress: Boolean = false,
    view: Model.View = Model.View.Metadata,
    metadata: MetadataModel = MetadataModel(),
    config: ConfigModel = ConfigModel()
):
  def getDate = metadata.dateTimeValidated

  def newEvent(zone: ZoneId): ValidatedNel[String, ServiceEvent.NewBikeEvent] =
    (
      metadata.brandValidated,
      metadata.nameValidated,
      metadata.description.validNel,
      metadata.dateTimeValidated
    ).mapN { (brand, name, descr, dt) =>
      ServiceEvent.NewBikeEvent(
        brandId = brand.id,
        name = name,
        description = descr,
        addedAt = dt.atZone(zone).toInstant,
        frontWheel = config.frontWheel,
        rearWheel = config.rearWheel,
        handlebar = config.handlebar,
        seatpost = config.seatpost,
        saddle = config.saddle,
        stem = config.stem,
        chain = config.chain,
        rearBrake = config.rearBrake,
        fork = config.fork,
        frontDerailleur = config.frontDerailleur,
        rearDerailleur = config.rearDerailleur,
        rearMudguard = config.rearMudguard
      )
    }

  def asBikeService(zoneId: ZoneId): ValidatedNel[String, BikeService] =
    newEvent(zoneId).map { ev =>
      BikeService(
        name = s"New bike: ${ev.name}",
        description = None,
        date = ev.addedAt,
        createdAt = None,
        totals = Nil,
        events = List(ev)
      )
    }

object Model:
  enum View:
    case Metadata
    case Config

  object View:
    given Eq[View] = Eq.fromUniversalEquals

  val view: Lens[Model, View] =
    Lens[Model, View](_.view)(a => _.copy(view = a))

  val metadata: Lens[Model, MetadataModel] =
    Lens[Model, MetadataModel](_.metadata)(a => _.copy(metadata = a))

  val config: Lens[Model, ConfigModel] =
    Lens[Model, ConfigModel](_.config)(a => _.copy(config = a))
