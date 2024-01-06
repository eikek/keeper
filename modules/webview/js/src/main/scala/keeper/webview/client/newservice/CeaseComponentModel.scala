package keeper.webview.client.newservice

import cats.data.ValidatedNel
import cats.syntax.all.*

import keeper.bikes.event.{ServiceEvent, ServiceEventName}
import keeper.webview.client.shared.ComponentTableSelect

import monocle.Lens

final case class CeaseComponentModel(
    selectModel: ComponentTableSelect.Model = ComponentTableSelect.Model(),
    withSubs: Boolean = false
) extends AsServiceEvent:

  val eventName: ServiceEventName = ServiceEventName.CeaseComponent
  def reset: ServiceEventModel => ServiceEventModel =
    ServiceEventModel.trashComponents.replace(CeaseComponentModel())

  def asServiceEvent: ValidatedNel[String, ServiceEvent] =
    selectModel.selectedComponents
      .map(ids => ServiceEvent.CeaseComponent(ids, withSubs))
      .toValidNel("Select some components")

object CeaseComponentModel:
  val select: Lens[CeaseComponentModel, ComponentTableSelect.Model] =
    Lens[CeaseComponentModel, ComponentTableSelect.Model](_.selectModel)(a =>
      _.copy(selectModel = a)
    )

  val withSubs: Lens[CeaseComponentModel, Boolean] =
    Lens[CeaseComponentModel, Boolean](_.withSubs)(a => _.copy(withSubs = a))
