package keeper.webview.client.components

import keeper.bikes.SimpleQuery
import keeper.bikes.data.ComponentWithProduct
import keeper.bikes.model.BikeBuilds

import monocle.Lens

final case class TableModel(
    bikes: BikeBuilds = BikeBuilds.empty,
    query: SimpleQuery = SimpleQuery.empty,
    components: List[ComponentWithProduct] = Nil,
    searchInProgress: Boolean = false
):
  def componentsWithTotals =
    components.map(c => (c, bikes.componentTotals.get(c.id)))

object TableModel:
  val bikes: Lens[TableModel, BikeBuilds] =
    Lens[TableModel, BikeBuilds](_.bikes)(a => _.copy(bikes = a))

  val query: Lens[TableModel, SimpleQuery] =
    Lens[TableModel, SimpleQuery](_.query)(a => _.copy(query = a))

  val queryString: Lens[TableModel, String] = query.andThen(SimpleQuery.text)

  val components: Lens[TableModel, List[ComponentWithProduct]] =
    Lens[TableModel, List[ComponentWithProduct]](_.components)(r =>
      m => m.copy(components = r, searchInProgress = false)
    )

  val searchInProgress: Lens[TableModel, Boolean] =
    Lens[TableModel, Boolean](_.searchInProgress)(a => _.copy(searchInProgress = a))

  val searchTrue: TableModel => TableModel = searchInProgress.replace(true)
  val searchFalse: TableModel => TableModel = searchInProgress.replace(false)
