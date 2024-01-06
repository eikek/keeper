package keeper.core

import cats.Show
import cats.effect.IO
import fs2.Stream

import munit.CatsEffectSuite

class MaintenanceBuildTest extends CatsEffectSuite with TestData {

  test("history") {
    val initial = MaintenanceBuild(
      Maintenance(MaintenanceId(0), DeviceTotals.empty, Seq.empty),
      defaultBuild
    )
    val ms = Stream.emits(
      List(
        Maintenance(
          MaintenanceId(1),
          DeviceTotals(bike1 -> TotalOutput(100)),
          events = List(
            ConfigEvent.ComponentRemove(bike1, chain1),
            ConfigEvent.ComponentAdd(bike1, chain4)
          )
        ),
        Maintenance(
          MaintenanceId(2),
          DeviceTotals(bike1 -> TotalOutput(300)),
          events = List(
            ConfigEvent.ComponentRemove(bike1, chain4),
            ConfigEvent.ComponentAdd(bike1, chain1)
          )
        )
      )
    )
    val current = DeviceTotals(bike1 -> TotalOutput(500), bike2 -> TotalOutput(400))

    val x = ms.through(ConfigBuilder.build(initial)).compile.toList
    println(
      "chain1: " + x.map(_.tracker.get(chain1)) + " last: " + x.last.tracker
        .finish(current)
        .get(chain1)
    )
    println(
      "chain4: " + x.map(_.tracker.get(chain4)) + " last: " + x.last.tracker
        .finish(current)
        .get(chain4)
    )

    val historyChain1 =
      ms.through(ConfigBuilder.build(initial))
        .through(ConfigBuilder.componentHistory[IO](current, chain1))
        .evalTap(IO.println)
        .compile
        .toList

    val historyChain4 =
      ms.through(ConfigBuilder.build(initial))
        .through(ConfigBuilder.componentHistory[IO](current, chain4))
        .evalTap(IO.println)
        .compile
        .toList

    assertIO(historyChain1.map(_.map(_.total)), List(TotalOutput(100), TotalOutput(200)))
    assertIO(historyChain4.map(_.map(_.total)), List(TotalOutput(200)))
  }

  given Show[MaintenanceBuild] =
    Show.show(a => s"M:${a.maintenance.id}, ${a.maintenance.events}")

  given Show[HistoryEntry] =
    Show.show(e =>
      s"HistoryEntry: Device:${e.deviceId} Added: ${e.added.maintenance.id} Removed:${e.removed
          .map(_.id)} Total: ${e.total}"
    )
}
