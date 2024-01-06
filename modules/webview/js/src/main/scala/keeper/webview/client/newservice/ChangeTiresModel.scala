package keeper.webview.client.newservice

import cats.Eq
import cats.data.ValidatedNel
import cats.syntax.all.*

import keeper.bikes.event.Alter.Discard
import keeper.bikes.event.{Alter, ServiceEvent, ServiceEventName}
import keeper.bikes.model.Bike
import keeper.core.{ComponentId, DeviceId}

import monocle.Lens

final case class ChangeTiresModel(
    bike: Option[DeviceId] = None,
    showMounted: Boolean = false,
    front: Alter[ComponentId] = Alter.Discard,
    rear: Alter[ComponentId] = Alter.Discard
) extends AsServiceEvent:
  val eventName: ServiceEventName = ServiceEventName.ChangeTires

  def asServiceEvent: ValidatedNel[String, ServiceEvent] =
    bikeValidated.andThen(w =>
      val ev = ServiceEvent.ChangeTires(w, front, rear)
      if ((front.asOption, rear.asOption).mapN(_ == _).contains(true))
        "Same tires cannot be used on two wheels".invalidNel
      else if (isModified) ev.validNel
      else "Nothing has been modified".invalidNel
    )

  val isModified: Boolean =
    List(front, rear).exists(!_.isDiscard)

  def reset: ServiceEventModel => ServiceEventModel =
    ServiceEventModel.tires.replace(ChangeTiresModel())

  val bikeValidated: ValidatedNel[String, DeviceId] =
    bike.toValidNel("You need to select a bike")

  def selectedBike(bikes: List[Bike]) =
    bike.flatMap(id => bikes.find(_.id == id))

object ChangeTiresModel:
  given Eq[ChangeTiresModel] = Eq.fromUniversalEquals

  val bike: Lens[ChangeTiresModel, Option[DeviceId]] =
    Lens[ChangeTiresModel, Option[DeviceId]](_.bike)(a => _.copy(bike = a))

  val showMounted: Lens[ChangeTiresModel, Boolean] =
    Lens[ChangeTiresModel, Boolean](_.showMounted)(a => _.copy(showMounted = a))

  val front: Lens[ChangeTiresModel, Alter[ComponentId]] =
    Lens[ChangeTiresModel, Alter[ComponentId]](_.front)(a => _.copy(front = a))

  val rear: Lens[ChangeTiresModel, Alter[ComponentId]] =
    Lens[ChangeTiresModel, Alter[ComponentId]](_.rear)(a => _.copy(rear = a))
