package keeper.webview.client.util

import keeper.bikes.data.DeviceWithBrand
import keeper.bikes.model.Bike

object BikeLabel {

  def apply(dev: DeviceWithBrand): String =
    s"${dev.brand.name} ${dev.device.name}"

  def apply(bike: Bike): String =
    s"${bike.brand.name} ${bike.name}"
}
