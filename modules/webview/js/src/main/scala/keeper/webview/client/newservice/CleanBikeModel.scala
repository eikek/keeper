package keeper.webview.client.newservice

import cats.data.ValidatedNel
import cats.syntax.all.*

import keeper.bikes.event.{ServiceEvent, ServiceEventName}
import keeper.webview.client.shared.BikeMultiSelect

import monocle.Lens

final case class CleanBikeModel(
    bikeSelect: BikeMultiSelect.Model = BikeMultiSelect.Model()
) extends AsServiceEvent:
  val eventName: ServiceEventName = ServiceEventName.CleanBike

  def reset: ServiceEventModel => ServiceEventModel =
    ServiceEventModel.cleanBike.replace(CleanBikeModel())

  def asServiceEvent: ValidatedNel[String, ServiceEvent] =
    bikeSelect.selectedBikes
      .map(ids => ServiceEvent.CleanBike(ids))
      .toValidNel("Select some bikes")

object CleanBikeModel:
  val bikes: Lens[CleanBikeModel, BikeMultiSelect.Model] =
    Lens[CleanBikeModel, BikeMultiSelect.Model](_.bikeSelect)(a => _.copy(bikeSelect = a))
