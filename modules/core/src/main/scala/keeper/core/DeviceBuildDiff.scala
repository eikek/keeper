package keeper.core

private object DeviceBuildDiff {
  final case class Result(
      devicesAdded: Set[DeviceId],
      devicesRemove: Set[DeviceId],
      devicesUnchanged: Set[DeviceId],
      componentsAdded: Set[ComponentId],
      componentsRemoved: Set[ComponentId],
      componentsUnchanged: Set[ComponentId]
  )

  def diff(a: DeviceBuild, b: DeviceBuild): Result =
    val deva = a.devices.keySet
    val devb = b.devices.keySet
    val compa = deviceComponents(a)
    val compb = deviceComponents(b)
    Result(
      devicesAdded = devb.diff(deva),
      devicesRemove = deva.diff(devb),
      devicesUnchanged = deva.intersect(devb),
      componentsAdded = compb.diff(compa),
      componentsRemoved = compa.diff(compb),
      componentsUnchanged = compa.intersect(compb)
    )

  private def deviceComponents(b: DeviceBuild) = {
    def getSubs(cid: ComponentId): Set[ComponentId] =
      val first = b.components.getOrElse(cid, Set.empty)
      first ++ first.flatMap(getSubs)

    val dc = b.devices.flatMap(_._2).toSet
    dc ++ dc.flatMap(getSubs)
  }
}
