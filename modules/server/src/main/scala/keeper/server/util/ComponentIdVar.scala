package keeper.server.util

import keeper.core.ComponentId

object ComponentIdVar {
  def unapply(s: String): Option[ComponentId] =
    s.toIntOption.map(ComponentId(_))
}
