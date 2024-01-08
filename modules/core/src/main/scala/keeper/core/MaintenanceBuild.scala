package keeper.core

final case class MaintenanceBuild(
    maintenance: Maintenance,
    build: DeviceBuild,
    tracker: TotalsTracker
):
  def totals(dev: DeviceId) = maintenance.totals.getOrZero(dev)

object MaintenanceBuild:
  def apply(maintenance: Maintenance, build: DeviceBuild): MaintenanceBuild =
    MaintenanceBuild(maintenance, build, TotalsTracker.init(build, maintenance.totals))

  def empty(
      id: MaintenanceId,
      initialTotals: Map[ComponentId, TotalOutput]
  ): MaintenanceBuild =
    MaintenanceBuild(
      Maintenance(id, DeviceTotals.empty, Seq.empty),
      DeviceBuild.empty,
      TotalsTracker.empty(initialTotals)
    )
