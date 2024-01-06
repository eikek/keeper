package keeper.core

final case class Maintenance(
    id: MaintenanceId,
    totals: DeviceTotals,
    events: Seq[ConfigEvent]
):
  def applyTo(build: DeviceBuild): DeviceBuild =
    build.applyAll(events)
