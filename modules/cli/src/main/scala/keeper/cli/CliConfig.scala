package keeper.cli

import java.time.ZoneId

import cats.MonadThrow
import cats.effect.Async
import cats.syntax.all.*

import keeper.bikes.Config
import keeper.bikes.db.PostgresConfig
import keeper.bikes.fit4s.Fit4sConfig
import keeper.bikes.strava.StravaConfig
import keeper.cli.config.LoggingConfig
import keeper.common.borer.BaseCodec.given

import io.bullet.borer.Encoder
import io.bullet.borer.NullOptions.*
import io.bullet.borer.derivation.MapBasedCodecs.*

final case class CliConfig(
    timezone: ZoneId,
    postgres: PostgresConfig,
    fit4sConfig: Option[Fit4sConfig],
    stravaConfig: Option[StravaConfig],
    logging: LoggingConfig
):
  def asBikeShopConfig: Config =
    Config(postgres, fit4sConfig, stravaConfig)

object CliConfig:
  private def cfg[F[_]: MonadThrow] =
    (
      ConfigValues.timeZone,
      ConfigValues.postgres,
      ConfigValues.fit4s.option,
      ConfigValues.strava.option,
      ConfigValues.logging
    ).mapN(CliConfig.apply)

  def load[F[_]: Async]: F[CliConfig] =
    cfg[F].load[F]

  given Encoder[CliConfig] = deriveEncoder
