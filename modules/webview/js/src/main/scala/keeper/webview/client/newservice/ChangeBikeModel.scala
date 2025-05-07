package keeper.webview.client.newservice

import cats.Eq
import cats.data.ValidatedNel
import cats.syntax.all.*

import keeper.bikes.data.ComponentType
import keeper.bikes.event.{Alter, ServiceEvent, ServiceEventName}
import keeper.bikes.model.Bike
import keeper.common.Lenses
import keeper.core.{ComponentId, DeviceId}

import monocle.Lens

final case class ChangeBikeModel(
    bike: Option[DeviceId] = None,
    showMounted: Boolean = false,
    frontWheel: Alter[ComponentId] = Alter.Discard,
    rearWheel: Alter[ComponentId] = Alter.Discard,
    handlebar: Alter[ComponentId] = Alter.Discard,
    seatpost: Alter[ComponentId] = Alter.Discard,
    saddle: Alter[ComponentId] = Alter.Discard,
    stem: Alter[ComponentId] = Alter.Discard,
    chain: Alter[ComponentId] = Alter.Discard,
    rearBrake: Alter[ComponentId] = Alter.Discard,
    fork: Alter[ComponentId] = Alter.Discard,
    frontDerailleur: Alter[ComponentId] = Alter.Discard,
    rearDerailleur: Alter[ComponentId] = Alter.Discard,
    rearMudguard: Alter[ComponentId] = Alter.Discard,
    crankSet: Alter[ComponentId] = Alter.Discard
) extends AsServiceEvent:
  def bikeValidated: ValidatedNel[String, DeviceId] =
    bike.toValidNel("You need to select a bike")

  def reset: ServiceEventModel => ServiceEventModel =
    ServiceEventModel.changeBike.replace(ChangeBikeModel())

  def asServiceEvent: ValidatedNel[String, ServiceEvent] =
    bikeValidated.andThen { dev =>
      val ev = ServiceEvent.ChangeBike(
        dev,
        frontWheel,
        rearWheel,
        handlebar,
        seatpost,
        saddle,
        stem,
        chain,
        rearBrake,
        fork,
        crankSet,
        frontDerailleur,
        rearDerailleur,
        rearMudguard
      )

      if (ev.isModified) ev.validNel
      else "Bike not changed, change something to save the service event".invalidNel
    }

  val eventName: ServiceEventName = ServiceEventName.ChangeBike

  def selectedBike(bikes: List[Bike]): Option[Bike] =
    bike.flatMap(id => bikes.find(_.id == id))

object ChangeBikeModel:
  given Eq[ChangeBikeModel] = Eq.fromUniversalEquals

  val bike: Lens[ChangeBikeModel, Option[DeviceId]] =
    Lens[ChangeBikeModel, Option[DeviceId]](_.bike)(a => _.copy(bike = a))

  val showMounted: Lens[ChangeBikeModel, Boolean] =
    Lens[ChangeBikeModel, Boolean](_.showMounted)(a => _.copy(showMounted = a))

  val chain: Lens[ChangeBikeModel, Alter[ComponentId]] =
    Lens[ChangeBikeModel, Alter[ComponentId]](_.chain)(a => _.copy(chain = a))

  val frontWheel: Lens[ChangeBikeModel, Alter[ComponentId]] =
    Lens[ChangeBikeModel, Alter[ComponentId]](_.frontWheel)(a => _.copy(frontWheel = a))

  val rearWheel: Lens[ChangeBikeModel, Alter[ComponentId]] =
    Lens[ChangeBikeModel, Alter[ComponentId]](_.rearWheel)(a => _.copy(rearWheel = a))

  val handlebar: Lens[ChangeBikeModel, Alter[ComponentId]] =
    Lens[ChangeBikeModel, Alter[ComponentId]](_.handlebar)(a => _.copy(handlebar = a))

  val seatpost: Lens[ChangeBikeModel, Alter[ComponentId]] =
    Lens[ChangeBikeModel, Alter[ComponentId]](_.seatpost)(a => _.copy(seatpost = a))

  val saddle: Lens[ChangeBikeModel, Alter[ComponentId]] =
    Lens[ChangeBikeModel, Alter[ComponentId]](_.saddle)(a => _.copy(saddle = a))

  val stem: Lens[ChangeBikeModel, Alter[ComponentId]] =
    Lens[ChangeBikeModel, Alter[ComponentId]](_.stem)(a => _.copy(stem = a))

  val rearBrake: Lens[ChangeBikeModel, Alter[ComponentId]] =
    Lens[ChangeBikeModel, Alter[ComponentId]](_.rearBrake)(a => _.copy(rearBrake = a))

  val fork: Lens[ChangeBikeModel, Alter[ComponentId]] =
    Lens[ChangeBikeModel, Alter[ComponentId]](_.fork)(a => _.copy(fork = a))

  val frontDerailleur: Lens[ChangeBikeModel, Alter[ComponentId]] =
    Lens[ChangeBikeModel, Alter[ComponentId]](_.frontDerailleur)(a =>
      _.copy(frontDerailleur = a)
    )

  val rearDerailleur: Lens[ChangeBikeModel, Alter[ComponentId]] =
    Lens[ChangeBikeModel, Alter[ComponentId]](_.rearDerailleur)(a =>
      _.copy(rearDerailleur = a)
    )

  val rearMudguard: Lens[ChangeBikeModel, Alter[ComponentId]] =
    Lens[ChangeBikeModel, Alter[ComponentId]](_.rearMudguard)(a =>
      _.copy(rearMudguard = a)
    )

  val crankSet: Lens[ChangeBikeModel, Alter[ComponentId]] =
    Lens[ChangeBikeModel, Alter[ComponentId]](_.crankSet)(a => _.copy(crankSet = a))

  def forType(ct: ComponentType): Lens[ChangeBikeModel, Alter[ComponentId]] = ct match
    case ComponentType.RearWheel       => rearWheel
    case ComponentType.FrontWheel      => frontWheel
    case ComponentType.Chain           => chain
    case ComponentType.Fork            => fork
    case ComponentType.RearBrake       => rearBrake
    case ComponentType.Stem            => stem
    case ComponentType.Handlebar       => handlebar
    case ComponentType.FrontDerailleur => frontDerailleur
    case ComponentType.RearDerailleur  => rearDerailleur
    case ComponentType.Seatpost        => seatpost
    case ComponentType.Saddle          => saddle
    case ComponentType.RearMudguard    => rearMudguard
    case ComponentType.CrankSet        => crankSet
    case ComponentType.FrontBrake      => Lenses.noop(Alter.Discard)
    case ComponentType.Cassette        => Lenses.noop(Alter.Discard)
    case ComponentType.BrakeDisc       => Lenses.noop(Alter.Discard)
    case ComponentType.Tire            => Lenses.noop(Alter.Discard)
    case ComponentType.InnerTube       => Lenses.noop(Alter.Discard)
    case ComponentType.FrontMudguard   => Lenses.noop(Alter.Discard)
    case ComponentType.BrakePad        => Lenses.noop(Alter.Discard)
