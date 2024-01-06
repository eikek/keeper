package keeper.webview.client.newservice

import cats.Eq
import cats.data.{NonEmptySet, ValidatedNel}
import cats.syntax.all.*

import keeper.bikes.event.{ServiceEvent, ServiceEventName}
import keeper.core.ComponentId
import keeper.webview.client.shared.ComponentSimpleSelect

import monocle.Lens

final case class PatchModel(
    eventName: ServiceEventName,
    mkEvent: NonEmptySet[ComponentId] => ServiceEvent,
    setSelf: PatchModel => ServiceEventModel => ServiceEventModel,
    components: ComponentSimpleSelect.Model = ComponentSimpleSelect.Model(),
    showMounted: Boolean = true
) extends AsServiceEvent:
  def asServiceEvent: ValidatedNel[String, ServiceEvent] =
    components.components.toList match
      case Nil    => "You need to select at least one tube".invalidNel
      case h :: t => mkEvent(NonEmptySet.of(h, t: _*)).validNel

  def reset: ServiceEventModel => ServiceEventModel =
    setSelf(PatchModel(eventName, mkEvent, setSelf))

object PatchModel:
  given Eq[PatchModel] = Eq.fromUniversalEquals

  val components: Lens[PatchModel, ComponentSimpleSelect.Model] =
    Lens[PatchModel, ComponentSimpleSelect.Model](_.components)(a =>
      _.copy(components = a)
    )

  val showMounted: Lens[PatchModel, Boolean] =
    Lens[PatchModel, Boolean](_.showMounted)(a => _.copy(showMounted = a))

  def forTubes: PatchModel =
    PatchModel(
      ServiceEventName.PatchTube,
      ServiceEvent.PatchTube.apply,
      ServiceEventModel.patchTube.replace
    )

  def forTires: PatchModel =
    PatchModel(
      ServiceEventName.PatchTire,
      ServiceEvent.PatchTire.apply,
      ServiceEventModel.patchTire.replace
    )
