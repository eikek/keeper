package keeper.webview.client.products

import keeper.bikes.data.ProductWithBrand
import keeper.bikes.{Page, SimpleQuery}

import monocle.Lens

final case class TableModel(
    query: SimpleQuery,
    products: List[ProductWithBrand],
    searchInProgress: Boolean
)

object TableModel:
  val empty: TableModel = TableModel(SimpleQuery("", Page(1000, 0)), Nil, false)

  val query: Lens[TableModel, SimpleQuery] =
    Lens[TableModel, SimpleQuery](_.query)(a => _.copy(query = a))

  val queryString: Lens[TableModel, String] = query.andThen(SimpleQuery.text)

  val products: Lens[TableModel, List[ProductWithBrand]] =
    Lens[TableModel, List[ProductWithBrand]](_.products)(r =>
      m => m.copy(products = r, searchInProgress = false)
    )

  val searchInProgress: Lens[TableModel, Boolean] =
    Lens[TableModel, Boolean](_.searchInProgress)(a => _.copy(searchInProgress = a))

  val searchTrue: TableModel => TableModel = searchInProgress.replace(true)
  val searchFalse: TableModel => TableModel = searchInProgress.replace(false)
