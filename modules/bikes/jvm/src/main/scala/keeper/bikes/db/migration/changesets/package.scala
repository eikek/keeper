package keeper.bikes.db.migration

package object changesets {
  val all: Seq[ChangeSet] = List(
    ComponentType.get,
    Product.brand,
    Product.product,
    DeviceComponent.get,
    Maintenance.action,
    Maintenance.maintenance,
    Maintenance.serviceEvent,
    Maintenance.maintenanceEvent,
    Maintenance.cache,
    Strava.auth,
    ComponentType.addCrankSet
  )
}
