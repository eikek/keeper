package keeper.webview.client.brands

import keeper.bikes.SimpleQuery
import keeper.bikes.data.Brand

import monocle.Lens

final case class TableModel(
    query: SimpleQuery = SimpleQuery.empty,
    brands: List[Brand] = Nil,
    searchInProgress: Boolean = false
)

object TableModel:
  val query: Lens[TableModel, SimpleQuery] =
    Lens[TableModel, SimpleQuery](_.query)(a => _.copy(query = a))

  val queryString: Lens[TableModel, String] = query.andThen(SimpleQuery.text)

  val brands: Lens[TableModel, List[Brand]] =
    Lens[TableModel, List[Brand]](_.brands)(r =>
      m => m.copy(brands = r, searchInProgress = false)
    )

  val searchInProgress: Lens[TableModel, Boolean] =
    Lens[TableModel, Boolean](_.searchInProgress)(a => _.copy(searchInProgress = a))

  val searchTrue: TableModel => TableModel = searchInProgress.replace(true)
  val searchFalse: TableModel => TableModel = searchInProgress.replace(false)
