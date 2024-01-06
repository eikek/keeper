package keeper.webview.client

import keeper.webview.client.brands.Model as BrandPageModel
import keeper.webview.client.components.Model as ComponentModel
import keeper.webview.client.dashboard.Model as DashboardModel
import keeper.webview.client.menu.TopBar
import keeper.webview.client.newbike.Model as NewBikeModel
import keeper.webview.client.newservice.Model as NewServiceModel
import keeper.webview.client.products.Model as ProductPageModel
import keeper.webview.client.strava.StravaSetupModel

import monocle.Lens

final case class AppModel(
    page: View,
    topBar: TopBar.Model,
    mainPage: DashboardModel,
    componentPage: ComponentModel,
    productPage: ProductPageModel,
    brandPage: BrandPageModel,
    newBikeForm: NewBikeModel,
    newServiceModel: NewServiceModel,
    stravaSetup: StravaSetupModel
)

object AppModel:

  val empty: AppModel = AppModel(
    View.Dashboard,
    TopBar.Model.empty,
    DashboardModel.empty,
    ComponentModel.empty,
    ProductPageModel.empty,
    BrandPageModel.empty,
    NewBikeModel(),
    NewServiceModel(),
    StravaSetupModel()
  )

  val page: Lens[AppModel, View] = Lens[AppModel, View](_.page)(a => _.copy(page = a))

  val topBar: Lens[AppModel, TopBar.Model] =
    Lens[AppModel, TopBar.Model](_.topBar)(a => _.copy(topBar = a))

  val dashboard: Lens[AppModel, DashboardModel] =
    Lens[AppModel, DashboardModel](_.mainPage)(a => _.copy(mainPage = a))

  val componentPage: Lens[AppModel, ComponentModel] =
    Lens[AppModel, ComponentModel](_.componentPage)(a => _.copy(componentPage = a))

  val productPage: Lens[AppModel, ProductPageModel] =
    Lens[AppModel, ProductPageModel](_.productPage)(a => _.copy(productPage = a))

  val brandPage: Lens[AppModel, BrandPageModel] =
    Lens[AppModel, BrandPageModel](_.brandPage)(a => _.copy(brandPage = a))

  val newBike: Lens[AppModel, NewBikeModel] =
    Lens[AppModel, NewBikeModel](_.newBikeForm)(a => _.copy(newBikeForm = a))

  val newService: Lens[AppModel, NewServiceModel] =
    Lens[AppModel, NewServiceModel](_.newServiceModel)(a => _.copy(newServiceModel = a))

  val stravaSetup: Lens[AppModel, StravaSetupModel] =
    Lens[AppModel, StravaSetupModel](_.stravaSetup)(a => _.copy(stravaSetup = a))
