package keeper.server

import java.time.ZoneId

import cats.effect.*
import cats.syntax.all.*
import fs2.io.net.Network

import keeper.bikes.BikeShop
import keeper.server.util.ErrorResponse

import com.comcast.ip4s.{Host, Port}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.middleware.{CORS, Logger}
import org.http4s.{HttpRoutes, Response}

object KeeperServer {

  def httpRoutes[F[_]: Async](
      bikeShop: BikeShop[F],
      zoneId: ZoneId
  ): HttpRoutes[F] = {
    val cors = CORS.policy.withAllowHeadersAll
    val routes = new Routes[F](bikeShop, zoneId)
    val logger = scribe.cats.effect[F]
    Logger.httpRoutes[F](
      logHeaders = true,
      logBody = false,
      logAction = Some(msg => logger.debug(msg))
    )(cors.httpRoutes[F](routes.all))
  }

  def apply[F[_]: Async: Network](
      host: Host,
      port: Port,
      bikeShop: BikeShop[F],
      zoneId: ZoneId
  ): Resource[F, Server] = {
    val logger = scribe.cats.effect[F]
    EmberServerBuilder
      .default[F]
      .withHost(host)
      .withPort(port)
      .withHttpApp(httpRoutes(bikeShop, zoneId).orNotFound)
      .withErrorHandler { case ex =>
        logger
          .error("Service raised an error!", ex)
          .as(ErrorResponse(ex))
      }
      .build
  }
}
