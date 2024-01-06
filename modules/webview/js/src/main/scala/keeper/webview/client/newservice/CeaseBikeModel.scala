package keeper.webview.client.newservice

import cats.Eq
import cats.data.ValidatedNel
import cats.syntax.all.*

import keeper.bikes.event.{ServiceEvent, ServiceEventName}
import keeper.bikes.model.Bike
import keeper.core.DeviceId

import monocle.Lens

final case class CeaseBikeModel(
    bike: Option[DeviceId] = None,
    withComps: Boolean = false
) extends AsServiceEvent:
  val eventName: ServiceEventName = ServiceEventName.CeaseBike

  def reset: ServiceEventModel => ServiceEventModel =
    ServiceEventModel.trashBike.replace(CeaseBikeModel())

  def asServiceEvent: ValidatedNel[String, ServiceEvent] =
    bikeValidated.map(id => ServiceEvent.CeaseBike(id, withComps))

  def bikeValidated: ValidatedNel[String, DeviceId] =
    bike.toValidNel("You need to select a bike")

  def selectedBike(bikes: List[Bike]): Option[Bike] =
    bike.flatMap(id => bikes.find(_.id == id))

object CeaseBikeModel:
  given Eq[CeaseBikeModel] = Eq.fromUniversalEquals

  val bike: Lens[CeaseBikeModel, Option[DeviceId]] =
    Lens[CeaseBikeModel, Option[DeviceId]](_.bike)(a => _.copy(bike = a))

  val withComps: Lens[CeaseBikeModel, Boolean] =
    Lens[CeaseBikeModel, Boolean](_.withComps)(a => _.copy(withComps = a))
