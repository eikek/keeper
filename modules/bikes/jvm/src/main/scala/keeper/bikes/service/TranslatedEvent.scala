package keeper.bikes.service

import keeper.bikes.data.NewMaintenanceEvent
import keeper.bikes.event.ServiceEvent
import keeper.core.DeviceBuild

final case class TranslatedEvent(
    serviceEvent: ServiceEvent,
    maintEvents: Seq[NewMaintenanceEvent],
    result: DeviceBuild
):
  def updateIndex(offset: Int): (Int, TranslatedEvent) =
    (
      offset + maintEvents.size,
      copy(maintEvents =
        maintEvents.zipWithIndex.map(t =>
          NewMaintenanceEvent.index.replace(offset + t._2)(t._1)
        )
      )
    )

  def updateEvents(f: NewMaintenanceEvent => NewMaintenanceEvent): TranslatedEvent =
    copy(maintEvents = maintEvents.map(f))
