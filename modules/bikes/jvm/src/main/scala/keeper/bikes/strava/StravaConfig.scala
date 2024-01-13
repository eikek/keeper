package keeper.bikes.strava

import scala.concurrent.duration.Duration

import keeper.common.borer.BaseCodec.given
import keeper.strava.{StravaAppCredentials, StravaClientConfig}

import io.bullet.borer.Encoder
import io.bullet.borer.derivation.MapBasedCodecs.deriveEncoder

final case class StravaConfig(
    credentials: StravaAppCredentials,
    clientConfig: StravaClientConfig,
    timeout: Duration
)

object StravaConfig:
  given Encoder[StravaConfig] = deriveEncoder
