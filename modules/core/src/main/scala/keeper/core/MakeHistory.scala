package keeper.core

import cats.Applicative
import cats.effect.Ref
import cats.syntax.all.*
import fs2.{Pipe, Stream}

private object MakeHistory {
  def componentHistory[F[_]: Ref.Make: Applicative](
      current: DeviceTotals,
      componentId: ComponentId
  ): Pipe[F, MaintenanceBuild, HistoryEntry] =
    in =>
      Stream
        .eval(Ref.of[F, State](State(None)))
        .flatMap { state =>
          in.evalMap { m =>
            val dev = m.build.findDevice(componentId)
            state.modify(_.record(dev, m))
          } ++ Stream.eval(state.get.map(_.finish(current)))
        }
        .unNone

  case class State(entry: Option[HistoryEntry]):
    def finish(current: DeviceTotals): Option[HistoryEntry] =
      entry.map(e =>
        e.copy(total = current.getOrZero(e.deviceId) - e.added.totals(e.deviceId))
      )

    def record(
        deviceId: Option[DeviceId],
        m: MaintenanceBuild
    ): (State, Option[HistoryEntry]) =
      (entry, deviceId) match
        case (None, None) => (this, None)

        // component got removed and added nowhere
        case (Some(e), None) =>
          val diff = m.totals(e.deviceId) - e.added.totals(e.deviceId)
          val entry = e.copy(removed = Some(m.maintenance), total = diff)
          (copy(entry = None), Some(entry))

        // component got added in this maintenance
        case (None, Some(dev)) =>
          val next = HistoryEntry(m, None, dev, TotalOutput.zero)
          (copy(entry = Some(next)), None)

        // component was moved to another device or stayed on the same device
        case (Some(e), Some(dev)) =>
          if (e.deviceId == dev) (this, None)
          else {
            val diff = m.totals(e.deviceId) - e.added.totals(e.deviceId)
            val entry = e.copy(removed = Some(m.maintenance), total = diff)
            val next = HistoryEntry(m, None, dev, TotalOutput.zero)
            (copy(entry = Some(next)), Some(entry))
          }
}
