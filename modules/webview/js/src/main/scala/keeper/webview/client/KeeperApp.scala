package keeper.webview.client

import java.time.ZoneId

import cats.effect.*
import cats.syntax.all.*
import fs2.concurrent.SignallingRef
import fs2.dom.HtmlElement

import keeper.client.{DefaultKeeperClient, KeeperClient}
import keeper.common.Lenses.syntax.*
import keeper.webview.client.brands.BrandPage
import keeper.webview.client.cmd.*
import keeper.webview.client.components.ComponentPage
import keeper.webview.client.dashboard.DashboardPage
import keeper.webview.client.menu.*
import keeper.webview.client.newbike.NewBikeForm
import keeper.webview.client.newservice.ServiceForm
import keeper.webview.client.products.ProductPage
import keeper.webview.client.strava.StravaSetupPage
import keeper.webview.client.util.DomContentLoaded

import calico.IOWebApp
import calico.html.io.{*, given}
import org.http4s.dom.FetchClientBuilder

object KeeperApp extends IOWebApp {
  private val logger = scribe.Logger("KeeperApp")
  // private val loggerF = scribe.cats.io

  val zoneId: ZoneId =
    try ZoneId.systemDefault()
    catch
      case e =>
        logger.warn(
          s"Cannot load system time zone: ${e.getMessage}. Use Europe/Berlin as fallback"
        )
        ZoneId.of("Europe/Berlin")

  // the base url is injected at build time
  val keeperClient: KeeperClient[IO] =
    new DefaultKeeperClient[IO](FetchClientBuilder[IO].create, BaseUrl.apply.get)

  private def subscribe(model: SignallingRef[IO, AppModel], cr: UiEventDispatch[IO]) =
    cr.subscribe.evalMap {
      case UiEvent.SetView(v) =>
        model.update(AppModel.page.replace(v))
      case _ =>
        IO.unit
    }

  override def render: Resource[IO, HtmlElement[IO]] =
    for {
      model <- SignallingRef[IO].of(AppModel.empty).toResource
      cr <- Resource.eval(UiEventDispatch[IO])
      _ <- subscribe(model, cr).compile.drain.background

      cnt <-
        div(
          cls := "parent flex flex-col",
          TopBar.render(model.to(AppModel.topBar), cr, zoneId),
          model.map(_.page).changes.map {
            case View.Brands =>
              BrandPage.render(model.to(AppModel.brandPage), keeperClient, cr, zoneId)

            case View.Dashboard =>
              val selectedDate = model.to(AppModel.topBar).map(_.dateValidated(zoneId))
              DashboardPage.render(
                keeperClient,
                model.to(AppModel.dashboard),
                selectedDate,
                cr,
                zoneId
              )

            case View.Components =>
              ComponentPage
                .render(model.to(AppModel.componentPage), keeperClient, cr, zoneId)

            case View.Products =>
              ProductPage.render(model.to(AppModel.productPage), keeperClient, cr, zoneId)

            case View.NewBike =>
              NewBikeForm.render(model.to(AppModel.newBike), keeperClient, cr, zoneId)

            case View.NewMaintenance =>
              ServiceForm.render(model.to(AppModel.newService), keeperClient, cr, zoneId)

            case View.StravaSetup =>
              StravaSetupPage
                .render(model.to(AppModel.stravaSetup), keeperClient, BaseUrl.apply)
          }
        )

      _ <- DomContentLoaded(cr).take(1).compile.drain.background
    } yield cnt
}
