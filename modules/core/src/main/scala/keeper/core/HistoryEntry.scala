package keeper.core

/** History entry regarding one component or subcomponent.
  *
  * @param added
  *   the maintenance when the item got added to the device
  * @param removed
  *   the maintenance when the item was removed from the device. if None, it is currently
  *   still mounted
  * @param total
  *   the total usage of the component on that device (between added and removed)
  */
final case class HistoryEntry(
    added: MaintenanceBuild,
    removed: Option[Maintenance],
    deviceId: DeviceId,
    total: TotalOutput
)
