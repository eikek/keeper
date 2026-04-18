package keeper.bikes.ventoux

import java.time.Instant
import java.time.format.DateTimeFormatter

import cats.data.OptionT
import cats.effect.*
import cats.syntax.all.*
import fs2.io.net.Network

import keeper.bikes.DistanceFinder
import keeper.bikes.data.Device
import keeper.bikes.model.BikeTotal
import keeper.bikes.util.DateUtil

import com.github.eikek.borer.compats.http4s.BorerEntityJsonCodec.given
import org.http4s.Method
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.ember.client.EmberClientBuilder

final class VentouxDistanceFinder[F[_]: Async](
    client: Client[F],
    config: VentouxConfig
) extends DistanceFinder[F]
    with Http4sClientDsl[F]:
  private val logger = scribe.cats.effect[F]
  private val localFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME

  def findDistanceAt(
      date: Instant,
      bikes: List[Device]
  ): F[Option[List[BikeTotal]]] =
    OptionT
      .whenM(DateUtil.isNotCurrent(date))(date.pure[F])
      .value
      .flatMap(findDistance(_, bikes))
      .flatTap(result => logger.info(s"Result: $result"))
      .map(_.some)

  def findDistance(date: Option[Instant], bikes: List[Device]): F[List[BikeTotal]] =
    val started =
      date.map(i => s"start<${localFormat.format(i.atZone(config.timezone))}")
    val url = (config.baseUrl / "api" / "activity" / "stats")
      .withOptionQueryParam("q", started)

    val req = Method
      .GET(url, ("Ventoux-Api-Key", config.apiKey))
    logger.info(s"Get distances from ventoux: $url") >>
      client.expect[VentouxModel.StatsResponse](req).map(_.toBikeTotal(bikes))

object VentouxDistanceFinder:
  def resource[F[_]: Async: Network](cfg: VentouxConfig): Resource[F, DistanceFinder[F]] =
    EmberClientBuilder
      .default[F]
      .withTimeout(cfg.timeout)
      .withIdleConnectionTime(cfg.timeout * 1.5)
      .build
      .map(c => new VentouxDistanceFinder[F](c, cfg))
