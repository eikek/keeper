package keeper.server.util

import keeper.bikes.data.ProductId

object ProductIdVar {
  def unapply(s: String): Option[ProductId] =
    s.toIntOption.map(ProductId(_))
}
