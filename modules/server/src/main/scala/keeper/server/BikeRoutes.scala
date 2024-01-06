package keeper.server

import java.time.ZoneId

import cats.data.EitherT
import cats.effect.{Clock, Sync}
import cats.syntax.all.*

import keeper.bikes.BikeShop
import keeper.bikes.model.{BikeService, ServiceSearchMask}
import keeper.server.util.*

import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

final class BikeRoutes[F[_]: Sync](
    shop: BikeShop[F],
    zoneId: ZoneId
) extends Http4sDsl[F]
    with MoreHttp4sDsl[F] {
  private val atVar = AtVar(zoneId)
  private val logger = scribe.cats.effect[F]

  def routes: HttpRoutes[F] = HttpRoutes.of {
    case GET -> Root :? atVar(tsOpt) =>
      tsOpt.withValid { n =>
        EitherT(shop.serviceBook.getBikesAt(n.some, zoneId))
          .foldF(
            err =>
              logger
                .error(s"Error getting bikes at $n: $err")
                .as(InternalServerError(err)),
            Ok(_)
          )
      }.orBadRequest

    case GET -> Root =>
      EitherT(shop.serviceBook.getBikesAt(None, zoneId))
        .foldF(
          err =>
            logger
              .error(s"Error getting current bikes: $err")
              .as(InternalServerError(err)),
          Ok(_)
        )

    case req @ POST -> Root / "service" =>
      for {
        in <- req.as[ServiceSearchMask]
        _ <- logger.debug(s"Searching maintenances: $in")
        res <- shop.serviceBook.getServiceDetail(in).compile.toVector
        resp <- Ok(res.reverse)
      } yield resp

    case GET -> Root / "service" :? atVar(at) +& PageVar(page) =>
      (at.fold(Clock[F].realTimeInstant.validNel)(_.map(_.pure[F])), page).mapN {
        (ts, p) =>
          for {
            until <- ts
            res <- shop.serviceBook.getServices(until.some, p)
            resp <- Ok(res)
          } yield resp
      }.orBadRequest

    case req @ POST -> Root / "service" / "submit" =>
      for {
        svc <- req.as[BikeService]
        res <- shop.serviceProvider.processBikeService(svc)
        resp <- res.fold(BadRequest(_), Ok(_))
      } yield resp

    case req @ POST -> Root / "service" / "preview" =>
      for {
        svc <- req.as[BikeService]
        res <- shop.serviceProvider.previewBikeService(svc.date, svc.events)
        resp <- res.fold(BadRequest(_), Ok(_))
      } yield resp

    case POST -> Root / "service" / "gencache" =>
      shop.serviceBook.generateMissingCacheEntries.flatMap(_ => NoContent())

    case GET -> Root / "distances" :? atVar(at) =>
      at.withValid { ts =>
        for {
          kms <- shop.serviceBook.getDistances(Some(ts), zoneId)
          resp <- Ok(kms)
        } yield resp
      }.orBadRequest

    case GET -> Root / "distances" =>
      for {
        kms <- shop.serviceBook.getDistances(None, zoneId)
        resp <- Ok(kms)
      } yield resp
  }
}
