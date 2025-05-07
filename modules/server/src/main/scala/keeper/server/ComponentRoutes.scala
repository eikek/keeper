package keeper.server

import java.time.ZoneId

import cats.effect.{Async, Clock}
import cats.syntax.all.*

import keeper.bikes.data.{ComponentWithProduct, NewComponent}
import keeper.bikes.db.Inventory
import keeper.client.data.*
import keeper.server.util.*

import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

final class ComponentRoutes[F[_]: Async](inventory: Inventory[F], zoneId: ZoneId)
    extends Http4sDsl[F]
    with MoreHttp4sDsl[F] {

  private val atVar = new AtVar(zoneId)

  def routes: HttpRoutes[F] = HttpRoutes.of {
    case req @ POST -> Root =>
      for {
        p <- req.as[NewComponent]
        id <- inventory.components.storeComponent(p)
        resp <- Ok(CreateResult(id.asLong))
      } yield resp

    case req @ PUT -> Root / ComponentIdVar(id) =>
      for {
        p <- req.as[NewComponent]
        ok <- inventory.components.updateComponent(id, p)
        resp <- Option
          .when(ok)(CreateResult(id.asLong))
          .orNotFound(s"No product with id $id")
      } yield resp

    case GET -> Root / "all" :? atVar(tsOpt) =>
      tsOpt match
        case Some(ts) =>
          ts.map(n => inventory.getComponentsAt(n).flatMap(Ok(_))).orBadRequest
        case None =>
          Clock[F].realTimeInstant.flatMap(n =>
            inventory.getComponentsAt(n).flatMap(Ok(_))
          )

    case GET -> Root :? ProductQueryParam(query) =>
      query
        .map(inventory.components.search)
        .map(Ok(_))
        .orBadRequest

  }
}
