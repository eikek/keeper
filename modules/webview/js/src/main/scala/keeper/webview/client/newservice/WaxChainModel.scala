package keeper.webview.client.newservice

import cats.Eq
import cats.data.{NonEmptySet, ValidatedNel}
import cats.syntax.all.*

import keeper.bikes.event.ServiceEvent.WaxChain.WaxType
import keeper.bikes.event.{ServiceEvent, ServiceEventName}
import keeper.core.ComponentId
import keeper.webview.client.shared.ComponentSimpleSelect

import monocle.Lens

final case class WaxChainModel(
    chains: ComponentSimpleSelect.Model = ComponentSimpleSelect.Model(),
    waxType: WaxType = WaxType.Drip,
    showMounted: Boolean = true
) extends AsServiceEvent:
  val eventName: ServiceEventName = ServiceEventName.WaxChain

  def asServiceEvent: ValidatedNel[String, ServiceEvent] =
    chains.components.toList match
      case Nil => "You need to select at least one chain".invalidNel
      case h :: t =>
        ServiceEvent.WaxChain(NonEmptySet.of(h, t: _*), waxType).validNel

  def reset: ServiceEventModel => ServiceEventModel =
    ServiceEventModel.waxChain.replace(WaxChainModel())

object WaxChainModel:
  given Eq[WaxChainModel] = Eq.fromUniversalEquals

  val chains: Lens[WaxChainModel, ComponentSimpleSelect.Model] =
    Lens[WaxChainModel, ComponentSimpleSelect.Model](_.chains)(a => _.copy(chains = a))

  val waxType: Lens[WaxChainModel, WaxType] =
    Lens[WaxChainModel, WaxType](_.waxType)(a => _.copy(waxType = a))

  val showMounted: Lens[WaxChainModel, Boolean] =
    Lens[WaxChainModel, Boolean](_.showMounted)(a => _.copy(showMounted = a))
