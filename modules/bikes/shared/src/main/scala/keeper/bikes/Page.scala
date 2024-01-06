package keeper.bikes

import monocle.Lens

final case class Page(limit: Int, offset: Long) {
  def isUnlimited: Boolean = this == Page.unlimited
}

object Page:
  val unlimited: Page = Page(Int.MaxValue, 0)

  def first(n: Int): Page = Page(n, 0)

  val limit: Lens[Page, Int] =
    Lens[Page, Int](_.limit)(a => _.copy(limit = a))

  val offset: Lens[Page, Long] =
    Lens[Page, Long](_.offset)(a => _.copy(offset = a))
