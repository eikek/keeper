package keeper.webview.client.dashboard

import cats.Eq

import keeper.bikes.data.ActionName
import keeper.bikes.model.ServiceDetail.{BikeAndName, ComponentInfo}
import keeper.bikes.model.{ServiceDetail, ServiceSearchMask}

import monocle.{Lens, Monocle}

final case class SearchMaskModel(
    searchBikes: Set[BikeAndName] = Set.empty,
    searchComps: Set[ComponentInfo] = Set.empty,
    searchActions: Set[ActionName] = Set.empty
):
  def isEmpty: Boolean =
    searchBikes.isEmpty && searchComps.isEmpty && searchActions.isEmpty
  def nonEmpty: Boolean = !isEmpty

  def toSearch: ServiceSearchMask =
    ServiceSearchMask(
      affectedBikes = searchBikes.map(_.id),
      affectedComponents = searchComps.map(_.id),
      actions = searchActions
    )

object SearchMaskModel:
  given Eq[SearchMaskModel] = Eq.fromUniversalEquals

  val searchBikes: Lens[SearchMaskModel, Set[BikeAndName]] =
    Lens[SearchMaskModel, Set[BikeAndName]](_.searchBikes)(a => _.copy(searchBikes = a))

  def searchBikesAt(b: BikeAndName) =
    searchBikes.andThen(Monocle.at[Set[BikeAndName], BikeAndName, Boolean](b))

  val searchComps: Lens[SearchMaskModel, Set[ComponentInfo]] =
    Lens[SearchMaskModel, Set[ComponentInfo]](_.searchComps)(a => _.copy(searchComps = a))

  def searchCompsAt(c: ComponentInfo) =
    searchComps.andThen(Monocle.at[Set[ComponentInfo], ComponentInfo, Boolean](c))

  val searchActions: Lens[SearchMaskModel, Set[ActionName]] =
    Lens[SearchMaskModel, Set[ActionName]](_.searchActions)(a =>
      _.copy(searchActions = a)
    )

  def searchActionAt(n: ActionName) =
    searchActions.andThen(Monocle.at[Set[ActionName], ActionName, Boolean](n))
