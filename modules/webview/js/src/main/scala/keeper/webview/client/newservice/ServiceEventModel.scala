package keeper.webview.client.newservice

import cats.data.ValidatedNel
import cats.syntax.all.*

import keeper.bikes.event.{ServiceEvent, ServiceEventName}

import monocle.Lens

final case class ServiceEventModel(
    active: ServiceEventName = ServiceEventName.ChangeBike,
    changeBike: ChangeBikeModel = ChangeBikeModel(),
    changeFrontWheel: ChangeWheelModel = ChangeWheelModel.frontWheel(),
    rearWheel: ChangeWheelModel = ChangeWheelModel.rearWheel(),
    fork: ChangeForkModel = ChangeForkModel(),
    tires: ChangeTiresModel = ChangeTiresModel(),
    pads: ChangeBrakePadsModel = ChangeBrakePadsModel(),
    waxChain: WaxChainModel = WaxChainModel(),
    trashComponents: CeaseComponentModel = CeaseComponentModel(),
    trashBike: CeaseBikeModel = CeaseBikeModel(),
    patchTube: PatchModel = PatchModel.forTubes,
    patchTire: PatchModel = PatchModel.forTires,
    cleanComponent: CleanComponentModel = CleanComponentModel(),
    cleanBike: CleanBikeModel = CleanBikeModel(),
    events: List[ServiceEvent] = Nil
):
  def activeForm: AsServiceEvent =
    active match
      case ServiceEventName.ChangeBike       => changeBike
      case ServiceEventName.ChangeFrontWheel => changeFrontWheel
      case ServiceEventName.ChangeRearWheel  => rearWheel
      case ServiceEventName.ChangeFork       => fork
      case ServiceEventName.ChangeTires      => tires
      case ServiceEventName.ChangeBrakePads  => pads
      case ServiceEventName.WaxChain         => waxChain
      case ServiceEventName.CeaseComponent   => trashComponents
      case ServiceEventName.CeaseBike        => trashBike
      case ServiceEventName.PatchTube        => patchTube
      case ServiceEventName.PatchTire        => patchTire
      case ServiceEventName.CleanComponent   => cleanComponent
      case ServiceEventName.CleanBike        => cleanBike
      case ServiceEventName.NewBike => AsServiceEvent.nothing(ServiceEventName.NewBike)

  def eventsValidated: ValidatedNel[String, List[ServiceEvent]] =
    events.reverse.validNel

object ServiceEventModel:
  val active: Lens[ServiceEventModel, ServiceEventName] =
    Lens[ServiceEventModel, ServiceEventName](_.active)(a => _.copy(active = a))

  def activeIs(en: ServiceEventName) =
    active.exist(_ == en)

  val events: Lens[ServiceEventModel, List[ServiceEvent]] =
    Lens[ServiceEventModel, List[ServiceEvent]](_.events)(a => _.copy(events = a))

  def prependEvent(e: ServiceEvent) =
    events.modify(list => e :: list)

  def removeEvent(e: ServiceEvent) =
    events.modify(list => list.filterNot(_ == e))

  val changeBike: Lens[ServiceEventModel, ChangeBikeModel] =
    Lens[ServiceEventModel, ChangeBikeModel](_.changeBike)(a => _.copy(changeBike = a))

  val changeFrontWheel: Lens[ServiceEventModel, ChangeWheelModel] =
    Lens[ServiceEventModel, ChangeWheelModel](_.changeFrontWheel)(a =>
      _.copy(changeFrontWheel = a)
    )

  val rearWheel: Lens[ServiceEventModel, ChangeWheelModel] =
    Lens[ServiceEventModel, ChangeWheelModel](_.rearWheel)(a => _.copy(rearWheel = a))

  val fork: Lens[ServiceEventModel, ChangeForkModel] =
    Lens[ServiceEventModel, ChangeForkModel](_.fork)(a => _.copy(fork = a))

  val tires: Lens[ServiceEventModel, ChangeTiresModel] =
    Lens[ServiceEventModel, ChangeTiresModel](_.tires)(a => _.copy(tires = a))

  val pads: Lens[ServiceEventModel, ChangeBrakePadsModel] =
    Lens[ServiceEventModel, ChangeBrakePadsModel](_.pads)(a => _.copy(pads = a))

  val waxChain: Lens[ServiceEventModel, WaxChainModel] =
    Lens[ServiceEventModel, WaxChainModel](_.waxChain)(a => _.copy(waxChain = a))

  val trashComponents: Lens[ServiceEventModel, CeaseComponentModel] =
    Lens[ServiceEventModel, CeaseComponentModel](_.trashComponents)(a =>
      _.copy(trashComponents = a)
    )

  val trashBike: Lens[ServiceEventModel, CeaseBikeModel] =
    Lens[ServiceEventModel, CeaseBikeModel](_.trashBike)(a => _.copy(trashBike = a))

  val patchTube: Lens[ServiceEventModel, PatchModel] =
    Lens[ServiceEventModel, PatchModel](_.patchTube)(a => _.copy(patchTube = a))

  val patchTire: Lens[ServiceEventModel, PatchModel] =
    Lens[ServiceEventModel, PatchModel](_.patchTire)(a => _.copy(patchTire = a))

  val cleanComponent: Lens[ServiceEventModel, CleanComponentModel] =
    Lens[ServiceEventModel, CleanComponentModel](_.cleanComponent)(a =>
      _.copy(cleanComponent = a)
    )

  val cleanBike: Lens[ServiceEventModel, CleanBikeModel] =
    Lens[ServiceEventModel, CleanBikeModel](_.cleanBike)(a => _.copy(cleanBike = a))
