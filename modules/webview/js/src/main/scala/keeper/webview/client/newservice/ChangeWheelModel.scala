package keeper.webview.client.newservice

import cats.Eq
import cats.data.ValidatedNel
import cats.syntax.all.*

import keeper.bikes.data.ComponentType
import keeper.bikes.event.Alter.Discard
import keeper.bikes.event.{Alter, ServiceEvent, ServiceEventName}
import keeper.bikes.model.BikePart
import keeper.common.Lenses
import keeper.core.ComponentId
import keeper.webview.client.newservice.ChangeWheelModel.WheelType

import monocle.Lens

final case class ChangeWheelModel private (
    wheelType: ChangeWheelModel.WheelType,
    wheel: Option[ComponentId] = None,
    showMounted: Boolean = false,
    disc: Alter[ComponentId] = Alter.Discard,
    tire: Alter[ComponentId] = Alter.Discard,
    tube: Alter[ComponentId] = Alter.Discard,
    cassette: Alter[ComponentId] = Alter.Discard
) extends AsServiceEvent:
  val eventName: ServiceEventName = wheelType match
    case WheelType.Rear  => ServiceEventName.ChangeRearWheel
    case WheelType.Front => ServiceEventName.ChangeFrontWheel

  def asServiceEvent: ValidatedNel[String, ServiceEvent] =
    wheelValidated.andThen(w =>
      val ev = wheelType match
        case WheelType.Front =>
          ServiceEvent.ChangeFrontWheel(w, tire, disc, tube).validNel
        case WheelType.Rear =>
          ServiceEvent.ChangeRearWheel(w, tire, disc, tube, cassette).validNel

      if (isModified) ev
      else "Nothing has been modified".invalidNel
    )

  val isModified: Boolean =
    List(disc, tire, tube, cassette).exists(!_.isDiscard)

  def reset: ServiceEventModel => ServiceEventModel =
    wheelType match
      case WheelType.Rear =>
        ServiceEventModel.rearWheel.replace(ChangeWheelModel(wheelType))
      case WheelType.Front =>
        ServiceEventModel.changeFrontWheel.replace(ChangeWheelModel(wheelType))

  val wheelValidated: ValidatedNel[String, ComponentId] =
    wheel.toValidNel("You need to select a wheel")

  def selectedPart[A <: BikePart](wheels: List[A]) =
    wheel.flatMap(id => wheels.find(_.id == id))

object ChangeWheelModel:
  given Eq[ChangeWheelModel] = Eq.fromUniversalEquals

  enum WheelType:
    case Front
    case Rear

  def frontWheel(
      wheel: Option[ComponentId] = None,
      disc: Alter[ComponentId] = Alter.Discard,
      tire: Alter[ComponentId] = Alter.Discard
  ): ChangeWheelModel =
    ChangeWheelModel(WheelType.Front, wheel, false, disc, tire, Alter.Discard)

  def rearWheel(
      wheel: Option[ComponentId] = None,
      disc: Alter[ComponentId] = Alter.Discard,
      tire: Alter[ComponentId] = Alter.Discard,
      cassette: Alter[ComponentId] = Discard
  ): ChangeWheelModel =
    ChangeWheelModel(WheelType.Rear, wheel, false, disc, tire, cassette)

  val wheel: Lens[ChangeWheelModel, Option[ComponentId]] =
    Lens[ChangeWheelModel, Option[ComponentId]](_.wheel)(a => _.copy(wheel = a))

  val showMounted: Lens[ChangeWheelModel, Boolean] =
    Lens[ChangeWheelModel, Boolean](_.showMounted)(a => _.copy(showMounted = a))

  val disc: Lens[ChangeWheelModel, Alter[ComponentId]] =
    Lens[ChangeWheelModel, Alter[ComponentId]](_.disc)(a => _.copy(disc = a))

  val tire: Lens[ChangeWheelModel, Alter[ComponentId]] =
    Lens[ChangeWheelModel, Alter[ComponentId]](_.tire)(a => _.copy(tire = a))

  val tube: Lens[ChangeWheelModel, Alter[ComponentId]] =
    Lens[ChangeWheelModel, Alter[ComponentId]](_.tube)(a => _.copy(tube = a))

  val cassette: Lens[ChangeWheelModel, Alter[ComponentId]] =
    Lens[ChangeWheelModel, Alter[ComponentId]](_.cassette)(a => _.copy(cassette = a))

  def forType(ct: ComponentType): Lens[ChangeWheelModel, Alter[ComponentId]] =
    ct match
      case ComponentType.BrakeDisc => disc
      case ComponentType.Tire      => tire
      case ComponentType.Cassette  => cassette
      case ComponentType.InnerTube => tube
      case _                       => Lenses.noop(Alter.Discard)
