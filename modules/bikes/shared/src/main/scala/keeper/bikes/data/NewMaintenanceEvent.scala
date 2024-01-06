package keeper.bikes.data

import cats.syntax.all.*

import keeper.core.*

import monocle.Lens

final case class NewMaintenanceEvent(
    maintenance: MaintenanceId,
    index: Int,
    action: ActionName,
    device: Option[DeviceId],
    component: Option[ComponentId],
    subComponent: Option[ComponentId]
)

object NewMaintenanceEvent:
  val maintenance: Lens[NewMaintenanceEvent, MaintenanceId] =
    Lens[NewMaintenanceEvent, MaintenanceId](_.maintenance)(a => _.copy(maintenance = a))

  val index: Lens[NewMaintenanceEvent, Int] =
    Lens[NewMaintenanceEvent, Int](_.index)(a => _.copy(index = a))

  def fromConfigEvent(maintenanceId: MaintenanceId, index: Int)(
      e: ConfigEvent
  ): NewMaintenanceEvent =
    e match
      case ConfigEvent.ComponentDrop(dev, id) =>
        NewMaintenanceEvent(
          maintenanceId,
          index,
          ActionName.Drop,
          dev,
          id.some,
          None
        )
      case ConfigEvent.DeviceDrop(dev) =>
        NewMaintenanceEvent(
          maintenanceId,
          index,
          ActionName.Drop,
          dev.some,
          None,
          None
        )

      case ConfigEvent.ComponentAdd(dev, comp) =>
        NewMaintenanceEvent(
          maintenanceId,
          index,
          ActionName.Add,
          dev.some,
          comp.some,
          None
        )
      case ConfigEvent.ComponentRemove(dev, comp) =>
        NewMaintenanceEvent(
          maintenanceId,
          index,
          ActionName.Remove,
          dev.some,
          comp.some,
          None
        )
      case ConfigEvent.SubComponentAdd(dev, parent, sub) =>
        NewMaintenanceEvent(
          maintenanceId,
          index,
          ActionName.Add,
          dev,
          parent.some,
          sub.some
        )
      case ConfigEvent.SubComponentRemove(dev, parent, sub) =>
        NewMaintenanceEvent(
          maintenanceId,
          index,
          ActionName.Remove,
          dev,
          parent.some,
          sub.some
        )
