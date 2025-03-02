package keeper.server

import java.time.ZoneId

import cats.effect.*

import keeper.bikes.BikeShop

import org.http4s.HttpRoutes
import org.http4s.server.Router

final class Routes[F[_]: Async](
    bikeShop: BikeShop[F],
    zoneId: ZoneId
) {

  private val components = new ComponentRoutes[F](bikeShop.inventory, zoneId)
  private val products = new ProductRoutes[F](bikeShop.inventory.products)
  private val brands = new BrandRoutes[F](bikeShop.inventory.brands)
  private val bikes = new BikeRoutes[F](bikeShop, zoneId)
  private val strava = new StravaRoutes[F](bikeShop)
  private val ui = new UiRoutes[F]

  val all: HttpRoutes[F] =
    Router(
      "api/brand" -> brands.routes,
      "api/product" -> products.routes,
      "api/component" -> components.routes,
      "api/bike" -> bikes.routes,
      "api/strava" -> strava.routes,
      "" -> ui.routes
    )
}
