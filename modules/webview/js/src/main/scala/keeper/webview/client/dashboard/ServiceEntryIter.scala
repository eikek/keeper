package keeper.webview.client.dashboard

import cats.data.NonEmptyList as Nel

import keeper.bikes.data.ActionName.AddRemoveOrCease
import keeper.bikes.data.{ActionName, ComponentType}
import keeper.bikes.model.ServiceDetail
import keeper.bikes.model.ServiceDetail.{BikeAndName, ComponentInfo, ServiceEntry}
import keeper.core.MaintenanceId

object ServiceEntryIter {

  final case class Diff(
      typ: ComponentType,
      added: List[ComponentInfo],
      removed: List[ComponentInfo],
      ceased: List[ComponentInfo]
  )
  final case class Action(
      name: ActionName,
      component: ComponentInfo | BikeAndName,
      previous: Option[ServiceDetail]
  )
  final case class ParentComp(comp: ComponentInfo, diffs: Nel[Diff])

  final case class Bike(
      bike: BikeAndName,
      ceased: Boolean,
      diffs: Nel[ParentComp | Diff | Action]
  )

  type FindPrevious =
    (MaintenanceId, ActionName, ComponentInfo | BikeAndName) => Option[ServiceDetail]
  object FindPrevious {
    val none: FindPrevious = (_, _, _) => None

    private def actionMatch(a: ActionName, b: ActionName) =
      (a == b) || {
        val ab = Set(a, b)
        Set(ActionName.HotWax, ActionName.DripWax).intersect(ab) == ab
      }

    def search(services: List[ServiceDetail]): FindPrevious = (id, action, comp) =>
      services
        .sortBy(-_.date.getEpochSecond)
        .dropWhile(_.id != id)
        .drop(1)
        .find(s =>
          s.entries.exists(e =>
            actionMatch(e.action, action) && (comp match {
              case i: ComponentInfo =>
                e.sub.map(_.id).contains(i.id) || e.component.map(_.id).contains(i.id)
              case b: BikeAndName => e.bike.map(_.id).contains(b.id)
            })
          )
        )
  }

  type Element = Bike | ParentComp | Action
  type Result = List[Element]

  def apply(s: ServiceDetail, findPrev: FindPrevious = FindPrevious.none): Result =
    s.entries
      .groupBy(_.bike)
      .flatMap {
        case (Some(bike), chunk) =>
          List(withBike(s.id, bike, Nel.fromListUnsafe(chunk), findPrev))
        case (None, chunk) => withoutBike(s.id, Nel.fromListUnsafe(chunk), findPrev)
      }
      .toList

  private def withBike(
      currentId: MaintenanceId,
      bike: BikeAndName,
      list: Nel[ServiceEntry],
      fp: FindPrevious
  ): Bike = {
    val ceased = list.exists(e =>
      e.action == ActionName.Cease && e.bike
        .contains(bike) && e.component.isEmpty && e.sub.isEmpty
    )

    val withSub =
      list
        .filter(_.sub.isDefined)
        .groupBy(_.component)
        .flatMap {
          case (Some(parent), chunk) =>
            withBikeAndSub(currentId, bike, parent, Nel.fromListUnsafe(chunk), fp)
          case (None, chunk) =>
            makeActions(currentId, chunk, _.flatMap(e => e.sub.toList), fp)
        }
        .toList

    val withoutSub =
      withBikeNoSub(currentId, list.filter(_.sub.isEmpty), fp)

    Bike(bike, ceased, Nel.fromListUnsafe(withoutSub ++ withSub))
  }

  private def withoutBike(
      currentId: MaintenanceId,
      list: Nel[ServiceEntry],
      findPrevious: FindPrevious
  ): List[ParentComp | Action] =
    list
      .groupBy(_.component)
      .flatMap {
        case (Some(parent), chunk) =>
          withCompNoBike(currentId, parent, chunk, findPrevious)
        case (None, chunk) => withoutBikeAndComp(currentId, chunk.toList, findPrevious)
      }
      .toList

  private def withBikeAndSub(
      currentId: MaintenanceId,
      bike: BikeAndName,
      comp: ComponentInfo,
      chunk: Nel[ServiceEntry],
      findPrevious: FindPrevious
  ): List[ParentComp | Action] = {
    val pc = makeParentComp(comp, chunk)
    val act = makeActions(
      currentId,
      chunk.filter(e => !e.action.isAddRemoveOrCease),
      _.flatMap(e => bike :: comp :: e.sub.toList),
      findPrevious
    )
    pc.toList ++ act
  }

  private def withBikeNoSub(
      currentId: MaintenanceId,
      chunk: List[ServiceEntry],
      findPrevious: FindPrevious
  ): List[Diff | Action] = {
    val diffs = makeDiffs(
      chunk.collect { case AddRemoveComp(t) => t }
    )
    val remain = makeActions(
      currentId,
      chunk.filter(e => AddRemoveComp.unapply(e).isEmpty),
      _.flatMap(e => e.component.orElse(e.bike).toList),
      findPrevious
    )
    diffs ++ remain
  }

  private def withCompNoBike(
      currentId: MaintenanceId,
      comp: ComponentInfo,
      chunk: Nel[ServiceEntry],
      findPrevious: FindPrevious
  ): List[ParentComp | Action] = {
    val pc = makeParentComp(comp, chunk).toList
    val remain = makeActions(
      currentId,
      chunk.toList.filter(e => AddRemoveSub.unapply(e).isEmpty),
      _.flatMap(e => comp :: e.sub.toList),
      findPrevious
    )
    pc ++ remain
  }

  private def withoutBikeAndComp(
      currentId: MaintenanceId,
      chunk: List[ServiceEntry],
      findPrevious: FindPrevious
  ): List[Action] =
    makeActions(currentId, chunk, _.flatMap(_.sub), findPrevious)

  private def makeParentComp(
      comp: ComponentInfo,
      chunk: Nel[ServiceEntry]
  ): Option[ParentComp] = {
    val diffs = makeDiffs(
      chunk.collect { case AddRemoveSub(t) => t }
    )
    Nel.fromList(diffs).map(ParentComp(comp, _))
  }

  private def makeDiffs(chunk: List[(ActionName, ComponentInfo)]): List[Diff] =
    chunk
      .groupBy(_._2.typ)
      .map { case (ct, list) =>
        val (rem, add, cease) =
          (
            list.filter(_._1 == ActionName.Remove),
            list.filter(_._1 == ActionName.Add),
            list.filter(_._1 == ActionName.Cease)
          )
        Diff(ct, add.map(_._2), rem.map(_._2), cease.map(_._2))
      }
      .toList

  private def makeActions(
      currentId: MaintenanceId,
      chunk: List[ServiceEntry],
      perAction: List[ServiceEntry] => List[ComponentInfo | BikeAndName],
      findPrevious: FindPrevious
  ): List[Action] =
    chunk
      .groupBy(_.action)
      .filter(_._1 != ActionName.Drop) // this is covered by ceased
      .flatMap { case (a, list) =>
        perAction(list).map(c => Action(a, c, findPrevious(currentId, a, c)))
      }
      .toList

  private object AddRemoveSub {
    def unapply(e: ServiceEntry): Option[(ActionName, ComponentInfo)] =
      e match
        case ServiceEntry(_, AddRemoveOrCease(an), _, _, _, Some(sub)) => Some(an -> sub)
        case _                                                         => None
  }

  private object AddRemoveComp {
    def unapply(e: ServiceEntry): Option[(ActionName, ComponentInfo)] =
      e match
        case ServiceEntry(_, AddRemoveOrCease(an), _, _, Some(comp), _) =>
          Some(an -> comp)
        case _ => None
  }
}
