package keeper.core

import munit.FunSuite

class TotalsTrackerTest extends FunSuite with TestData {

  test("scenario: add, remove, add, keep") {
    val events =
      List(
        Maintenance(
          MaintenanceId(1),
          DeviceTotals(bike1 -> TotalOutput(1369.32)),
          events = List(
            ConfigEvent.ComponentAdd(bike1, chain1)
          )
        ),
        Maintenance(
          MaintenanceId(2),
          DeviceTotals(bike1 -> TotalOutput(2647.0)),
          events = List(
            ConfigEvent.ComponentRemove(bike1, chain1)
          )
        ),
        Maintenance(
          MaintenanceId(4),
          DeviceTotals.empty,
          events = List(
            ConfigEvent.ComponentAdd(bike2, chain2)
          )
        ),
        Maintenance(
          MaintenanceId(5),
          DeviceTotals(bike1 -> TotalOutput(3872.34)),
          events = List(ConfigEvent.ComponentAdd(bike1, chain1))
        ),
        Maintenance(
          MaintenanceId(4),
          DeviceTotals.empty,
          events = List(
            ConfigEvent.ComponentAdd(bike2, chain2)
          )
        ),
        Maintenance(
          MaintenanceId(6),
          DeviceTotals(bike1 -> TotalOutput(5433.84)),
          events = List()
        )
      )

    val tt =
      events.scanLeft((TotalsTracker.empty(Map.empty), DeviceBuild.empty)) {
        case ((tracker, build), m) =>
          val next = m.applyTo(build)
          (tracker.record(next, m.totals), next)
      }

    assertEquals(
      tt.map(_._1.get(chain1).map(_.asDouble.toInt)),
      List(None, Some(0), Some(1277), Some(1277), Some(1277), Some(1277), Some(2839))
    )
  }
}
