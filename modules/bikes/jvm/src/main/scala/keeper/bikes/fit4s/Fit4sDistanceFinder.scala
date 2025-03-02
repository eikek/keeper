package keeper.bikes.fit4s

import java.time.Instant

import cats.data.OptionT
import cats.effect.{Async, Resource, Sync}
import cats.syntax.all.*
import fs2.io.net.Network

import keeper.bikes.DistanceFinder
import keeper.bikes.data.Device
import keeper.bikes.model.BikeTotal
import keeper.bikes.util.DateUtil
import keeper.common.Distance
import keeper.http.borer.BorerEntityCodec.Implicits.*

import org.http4s.Method
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.ember.client.EmberClientBuilder

final class Fit4sDistanceFinder[F[_]: Sync](
    client: Client[F],
    config: Fit4sConfig
) extends DistanceFinder[F]
    with Http4sClientDsl[F] {
  private val logger = scribe.cats.effect[F]

  def findDistanceAt(
      date: Instant,
      bikes: List[Device]
  ): F[Option[List[BikeTotal]]] =
    OptionT
      .whenM(DateUtil.isNotCurrent(date))(date.pure[F])
      .value
      .flatMap(ts => findDistances(ts, bikes).map(_.some))

  def findDistances(
      date: Option[Instant],
      bikes: List[Device]
  ): F[List[BikeTotal]] =
    bikes.traverse(findDistance(date, _)).map(_.flatten).attempt.flatMap {
      case Right(r) => r.pure[F]
      case Left(err) =>
        logger
          .error(
            s"Error obtaining distance data from fit4s at ${config.baseUrl.renderString}",
            err
          )
          .as(Nil)
    }

  def findDistance(date: Option[Instant], bike: Device): F[Option[BikeTotal]] =
    val started = date
      .map(i => s" started<${i.getEpochSecond}s")
      .getOrElse("")
    val url = (config.baseUrl / "api" / "activity" / "summary")
      .withQueryParam("q", s"tag=\"Bike/${bike.name}\" $started")
    logger.info(s"Get distances from fit4s: $url") >>
      client
        .expect[List[Fit4sSummary]](Method.GET(url))
        .map(_.headOption.map { s =>
          BikeTotal(bike.id, Distance.meter(s.distance.meter))
        })
}

object Fit4sDistanceFinder:
  def resource[F[_]: Async: Network](cfg: Fit4sConfig): Resource[F, DistanceFinder[F]] =
    EmberClientBuilder
      .default[F]
      .withTimeout(cfg.timeout)
      .withIdleConnectionTime(cfg.timeout * 1.5)
      .build
      .map(c => new Fit4sDistanceFinder[F](c, cfg))
