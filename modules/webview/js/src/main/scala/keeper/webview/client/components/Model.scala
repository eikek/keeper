package keeper.webview.client.components

import monocle.Lens

final case class Model(form: FormModel, table: TableModel)

object Model:
  val empty: Model = Model(FormModel(), TableModel())

  val form: Lens[Model, FormModel] =
    Lens[Model, FormModel](_.form)(a => _.copy(form = a))

  val table: Lens[Model, TableModel] =
    Lens[Model, TableModel](_.table)(a => _.copy(table = a))
