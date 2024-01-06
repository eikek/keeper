package keeper.webview.client.cmd

import cats.Eq

import keeper.bikes.data.*
import keeper.core.ComponentId
import keeper.webview.client.View

enum UiEvent:
  case DomContentLoaded
  case SetView(view: View)
  case BikeProductCreated(id: ProductId, product: NewBikeProduct)
  case BikeProductUpdated(id: ProductId, product: NewBikeProduct)
  case BikeProductEditRequest(product: ProductWithBrand)
  case BikeProductCopyRequest(product: ProductWithBrand)
  case BikeComponentUpdated(id: ComponentId, comp: NewComponent)
  case BikeComponentCreated(id: ComponentId, comp: NewComponent)
  case BikeComponentEditRequest(product: ComponentWithProduct)
  case BikeComponentCopyRequest(product: ComponentWithProduct)
  case BikeBrandCreated(id: BrandId, brand: NewBrand)
  case BikeBrandUpdated(id: BrandId, brand: NewBrand)
  case BikeBrandEditRequest(brand: Brand)

object UiEvent:
  given Eq[UiEvent] = Eq.fromUniversalEquals
