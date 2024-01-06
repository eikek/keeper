package keeper.bikes.strava

import cats.effect.{Async, Resource}
import fs2.io.file.Files
import fs2.io.net.Network

import keeper.strava.impl.DefaultStravaService
import keeper.strava.{StravaClient, StravaService, TokenStore}

import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import skunk.Session

object StravaService {

  def httpClient[F[_]: Async: Network](cfg: StravaConfig) =
    EmberClientBuilder
      .default[F]
      .withTimeout(cfg.timeout)
      .withIdleConnectionTime(cfg.timeout * 1.5)
      .build

  def client[F[_]: Async: Network: Files](
      cfg: StravaConfig,
      client: Client[F]
  ): F[StravaClient[F]] =
    StravaClient(cfg.clientConfig, client)

  def resource[F[_]: Async: Network: Files](
      cfg: StravaConfig,
      tokenStore: TokenStore[F]
  ): Resource[F, StravaService[F]] =
    httpClient(cfg)
      .evalMap(c => client(cfg, c))
      .map(stravaClient =>
        new DefaultStravaService(cfg.credentials, stravaClient, tokenStore)
      )

  def resource[F[_]: Async: Network: Files](
      cfg: StravaConfig,
      pool: Resource[F, Session[F]]
  ): Resource[F, StravaService[F]] =
    resource(cfg, new StravaTokenStore[F](pool))
}
