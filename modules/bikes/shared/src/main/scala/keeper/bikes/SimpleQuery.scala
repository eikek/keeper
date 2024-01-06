package keeper.bikes

import monocle.Lens

final case class SimpleQuery(text: String, page: Page)

object SimpleQuery:
  val empty: SimpleQuery = SimpleQuery("", Page.first(1000))

  val text: Lens[SimpleQuery, String] =
    Lens[SimpleQuery, String](_.text)(a => _.copy(text = a))

  val page: Lens[SimpleQuery, Page] =
    Lens[SimpleQuery, Page](_.page)(a => _.copy(page = a))
