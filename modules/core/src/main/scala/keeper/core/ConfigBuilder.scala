package keeper.core

import cats.Applicative
import cats.effect.Ref
import fs2.Pipe

object ConfigBuilder {

  def build[F[_]](initial: MaintenanceBuild): Pipe[F, Maintenance, MaintenanceBuild] =
    _.scan(initial) { (build, maint) =>
      val next = maint.applyTo(build.build)
      val totals = build.tracker.record(next, maint.totals)
      MaintenanceBuild(maint, next, totals)
    }

  def componentHistory[F[_]: Ref.Make: Applicative](
      current: DeviceTotals,
      componentId: ComponentId
  ): Pipe[F, MaintenanceBuild, HistoryEntry] =
    MakeHistory.componentHistory(current, componentId)
}
