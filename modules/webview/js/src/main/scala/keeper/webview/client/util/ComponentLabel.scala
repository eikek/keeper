package keeper.webview.client.util

import keeper.bikes.data.{ComponentWithDevice, ComponentWithProduct}
import keeper.bikes.model.BikePart

object ComponentLabel {

  def apply(c: ComponentWithDevice): String =
    apply(c.component)

  def apply(c: ComponentWithProduct): String =
    s"${c.component.name} (${c.brand.name} ${c.product.name})"

  def apply(p: BikePart): String =
    s"${p.name} (${p.product.brand.name} ${p.product.product.name})"
}
