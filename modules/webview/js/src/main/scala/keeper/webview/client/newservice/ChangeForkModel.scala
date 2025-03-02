package keeper.webview.client.newservice

import cats.Eq
import cats.data.ValidatedNel
import cats.syntax.all.*

import keeper.bikes.data.ComponentType
import keeper.bikes.event.{Alter, ServiceEvent, ServiceEventName}
import keeper.bikes.model.BikePart
import keeper.common.Lenses
import keeper.core.ComponentId

import monocle.Lens

final case class ChangeForkModel(
    fork: Option[ComponentId] = None,
    showMounted: Boolean = false,
    mudguard: Alter[ComponentId] = Alter.Discard,
    brake: Alter[ComponentId] = Alter.Discard
) extends AsServiceEvent:
  val eventName: ServiceEventName = ServiceEventName.ChangeFork

  def asServiceEvent: ValidatedNel[String, ServiceEvent] =
    forkValidated.andThen(w =>
      val ev = ServiceEvent.ChangeFork(w, mudguard, brake)

      if (isModified) ev.validNel
      else "Nothing has been modified".invalidNel
    )

  val isModified: Boolean =
    List(brake, mudguard).exists(!_.isDiscard)

  def reset: ServiceEventModel => ServiceEventModel =
    ServiceEventModel.fork.replace(ChangeForkModel())

  val forkValidated: ValidatedNel[String, ComponentId] =
    fork.toValidNel("You need to select a fork")

  def selectedPart[A <: BikePart](wheels: List[A]) =
    fork.flatMap(id => wheels.find(_.id == id))

object ChangeForkModel:
  given Eq[ChangeForkModel] = Eq.fromUniversalEquals

  val fork: Lens[ChangeForkModel, Option[ComponentId]] =
    Lens[ChangeForkModel, Option[ComponentId]](_.fork)(a => _.copy(fork = a))

  val showMounted: Lens[ChangeForkModel, Boolean] =
    Lens[ChangeForkModel, Boolean](_.showMounted)(a => _.copy(showMounted = a))

  val brake: Lens[ChangeForkModel, Alter[ComponentId]] =
    Lens[ChangeForkModel, Alter[ComponentId]](_.brake)(a => _.copy(brake = a))

  val mudguard: Lens[ChangeForkModel, Alter[ComponentId]] =
    Lens[ChangeForkModel, Alter[ComponentId]](_.mudguard)(a => _.copy(mudguard = a))

  def forType(ct: ComponentType): Lens[ChangeForkModel, Alter[ComponentId]] =
    ct match
      case ComponentType.FrontMudguard => mudguard
      case ComponentType.FrontBrake    => brake
      case _                           => Lenses.noop(Alter.Discard)
