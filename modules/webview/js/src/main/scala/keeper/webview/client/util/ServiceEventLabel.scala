package keeper.webview.client.util

import keeper.bikes.data.ComponentType
import keeper.bikes.event.ServiceEvent
import keeper.core.DeviceId

object ServiceEventLabel {

  def apply(
      event: ServiceEvent,
      findBike: DeviceId => Option[String] = _ => None
  ): String =
    event match
      case ev: ServiceEvent.ChangeBike =>
        val bikeName = findBike(ev.bike).map(n => s" $n").getOrElse("")
        val changedParts =
          ComponentType.values.toSeq
            .collect {
              case ct if !ev.forType(ct).isDiscard => ct
            }
            .mkString(", ")

        s"${ev.eventName.label}$bikeName: $changedParts"

      case ev: ServiceEvent.NewBikeEvent =>
        val cts = ev.componentTypes.map(_._2).toSet.mkString(", ")
        s"${ev.eventName.label} with $cts"

      case ev: ServiceEvent.CeaseComponent =>
        s"${ev.eventName.label}: ${ev.components.toNonEmptyList.size}"

      case _ =>
        val cts = event.componentTypes.map(_._2).toSet.mkString(", ")
        s"${event.eventName.label}: $cts"
}
