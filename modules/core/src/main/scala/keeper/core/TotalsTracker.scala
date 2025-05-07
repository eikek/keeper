package keeper.core

import keeper.core.TotalsTracker.{Entry, EntryMap}

import io.bullet.borer.NullOptions.given
import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.{Decoder, Encoder}

final class TotalsTracker(
    last: DeviceBuild,
    val totals: EntryMap
) {
  override def toString: String = s"TotalsTracker(last=$last, totals=$totals)"

  def get(cid: ComponentId): Option[TotalOutput] =
    totals.get(cid).map(_.running)

  def record(next: DeviceBuild, nextTotals: DeviceTotals): TotalsTracker =
    def nextTotalsFor(cid: ComponentId) =
      TotalsTracker.totalsFor(cid, next, nextTotals)

    def lastTotalsFor(cid: ComponentId) =
      TotalsTracker.totalsFor(cid, last, nextTotals)

    val diff = DeviceBuildDiff.diff(last, next)
    val removed =
      diff.componentsRemoved.foldLeft(totals) { (res, cid) =>
        remove(cid, lastTotalsFor(cid))(res)
      }

    val added =
      diff.componentsAdded.foldLeft(removed) { (res, cid) =>
        add(cid, nextTotalsFor(cid))(res)
      }

    val newTotals =
      diff.componentsUnchanged.foldLeft(added) { (res, cid) =>
        accumulateUnchanged(cid, nextTotalsFor(cid))(res)
      }
    new TotalsTracker(next, newTotals)

  def finish(current: DeviceTotals): Map[ComponentId, TotalOutput] =
    def totalsFor(cid: ComponentId) = TotalsTracker.totalsFor(cid, last, current)
    totals.keySet
      .foldLeft(totals) { (res, cid) =>
        remove(cid, totalsFor(cid))(res)
      }
      .view
      .mapValues(_.running)
      .toMap

  private def accumulateUnchanged(cid: ComponentId, total: TotalOutput)(
      totals: Map[ComponentId, Entry]
  ) =
    totals.get(cid) match
      case Some(Entry(_, running, Some(addedAt))) if total > TotalOutput.zero =>
        val diff = (total - addedAt).positiveOrZero
        totals.updated(cid, Entry(cid, running + diff, Some(total)))
      case _ =>
        totals

  private def add(cid: ComponentId, total: TotalOutput)(totals: Map[ComponentId, Entry]) =
    totals.get(cid) match
      case Some(e) =>
        totals.updated(cid, e.copy(addedAt = Some(total)))
      case None =>
        totals.updated(cid, Entry(cid, TotalOutput.zero, Some(total)))

  private def remove(cid: ComponentId, total: TotalOutput)(
      totals: Map[ComponentId, Entry]
  ) =
    totals.get(cid) match
      case Some(Entry(_, running, Some(addedAt))) =>
        val diff = (total - addedAt).positiveOrZero
        totals.updated(cid, Entry(cid, running + diff, None))
      case _ =>
        totals
}

object TotalsTracker:
  def init(build: DeviceBuild, totals: DeviceTotals): TotalsTracker =
    val newTotals = DeviceBuild
      .allComponentIds(build)
      .map(cid =>
        cid -> Entry(
          cid,
          TotalOutput.zero,
          Some(totalsFor(cid, build, totals))
        )
      )
      .toMap
    new TotalsTracker(build, newTotals)

  def empty(initialTotals: Map[ComponentId, TotalOutput]): TotalsTracker =
    new TotalsTracker(
      DeviceBuild.empty,
      initialTotals.view.map { case (k, v) => k -> Entry(k, v, None) }.toMap
    )

  private def totalsFor(cid: ComponentId, build: DeviceBuild, totals: DeviceTotals) =
    build.findDevice(cid).map(totals.getOrZero).getOrElse(TotalOutput.zero)

  final case class Entry(
      id: ComponentId,
      running: TotalOutput,
      addedAt: Option[TotalOutput]
  )
  object Entry:
    given Encoder[Entry] = deriveEncoder
    given Decoder[Entry] = deriveDecoder

  type EntryMap = Map[ComponentId, Entry]
  object EntryMap:
    given Encoder[EntryMap] =
      Encoder.of[List[(ComponentId, Entry)]].contramap(_.toList)
    given Decoder[EntryMap] =
      Decoder.of[List[(ComponentId, Entry)]].map(_.toMap)
