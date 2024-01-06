package keeper.webview.client.dashboard

import cats.Eq

import keeper.bikes.model.{BikeBuilds, BikeTotal, ServiceDetail}
import keeper.common.Distance
import keeper.core.{ComponentId, DeviceId}

import monocle.Lens

final case class Model(
    bikes: BikeBuilds,
    detailsVisible: Set[DeviceId],
    serviceList: List[ServiceDetail],
    searchMaskModel: SearchMaskModel
)

object Model:
  val empty: Model = Model(BikeBuilds.empty, Set.empty, Nil, SearchMaskModel())
  given Eq[Model] = Eq.fromUniversalEquals

  val bikes: Lens[Model, BikeBuilds] =
    Lens[Model, BikeBuilds](_.bikes)(a => _.copy(bikes = a))
  val detailsVisible: Lens[Model, Set[DeviceId]] =
    Lens[Model, Set[DeviceId]](_.detailsVisible)(a => _.copy(detailsVisible = a))

  val bikeTotalsList: Lens[Model, List[BikeTotal]] =
    bikes.andThen(BikeBuilds.bikeTotals)

  val bikeTotalsMap: Lens[Model, Map[DeviceId, Distance]] =
    bikeTotalsList.andThen(
      Lens[List[BikeTotal], Map[DeviceId, Distance]](
        _.map(b => b.bikeId -> b.distance).toMap
      )(m => _ => m.toList.map(BikeTotal.apply.tupled))
    )

  val componentTotals: Lens[Model, Map[ComponentId, Distance]] =
    bikes.andThen(BikeBuilds.componentTotals)

  def totalsFor(id: DeviceId) =
    bikeTotalsList.andThen(
      Lens[List[BikeTotal], Option[Distance]](_.find(_.bikeId == id).map(_.distance))(
        dst =>
          list =>
            dst
              .map(d => BikeTotal(id, d) :: list.filterNot(_.bikeId == id))
              .getOrElse(list.filterNot(_.bikeId == id))
      )
    )

  val searchMask: Lens[Model, SearchMaskModel] =
    Lens[Model, SearchMaskModel](_.searchMaskModel)(a => _.copy(searchMaskModel = a))

  val serviceList: Lens[Model, List[ServiceDetail]] =
    Lens[Model, List[ServiceDetail]](_.serviceList)(a => _.copy(serviceList = a))

  def toggle(id: DeviceId) =
    detailsVisible.modify(s => if (s.contains(id)) s - id else s + id)

  def detailFor(id: DeviceId) = detailsVisible.at(id)
