package keeper.server.util

import keeper.bikes.data.BrandId

object BrandIdVar {

  def unapply(s: String): Option[BrandId] =
    s.toIntOption.map(BrandId(_))
}
