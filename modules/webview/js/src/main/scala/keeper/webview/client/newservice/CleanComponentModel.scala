package keeper.webview.client.newservice

import cats.data.ValidatedNel
import cats.syntax.all.*

import keeper.bikes.event.{ServiceEvent, ServiceEventName}
import keeper.webview.client.shared.ComponentTableSelect

import monocle.Lens

final case class CleanComponentModel(
    selectModel: ComponentTableSelect.Model = ComponentTableSelect.Model()
) extends AsServiceEvent:

  val eventName: ServiceEventName = ServiceEventName.CleanComponent

  def reset: ServiceEventModel => ServiceEventModel =
    ServiceEventModel.cleanComponent.replace(CleanComponentModel())

  def asServiceEvent: ValidatedNel[String, ServiceEvent] =
    selectModel.selectedComponents
      .map(ids => ServiceEvent.CleanComponent(ids))
      .toValidNel("Select some components")

object CleanComponentModel:
  val select: Lens[CleanComponentModel, ComponentTableSelect.Model] =
    Lens[CleanComponentModel, ComponentTableSelect.Model](_.selectModel)(a =>
      _.copy(selectModel = a)
    )
