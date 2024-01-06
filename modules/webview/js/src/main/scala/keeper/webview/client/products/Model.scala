package keeper.webview.client.products

import monocle.Lens

final case class Model(
    table: TableModel,
    form: FormModel
)

object Model:
  val empty: Model = Model(TableModel.empty, FormModel.empty)

  val table: Lens[Model, TableModel] =
    Lens[Model, TableModel](_.table)(a => _.copy(table = a))

  val form: Lens[Model, FormModel] =
    Lens[Model, FormModel](_.form)(a => _.copy(form = a))
