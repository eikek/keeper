package keeper.bikes.strava

import scala.concurrent.duration.Duration

import keeper.strava.{StravaAppCredentials, StravaClientConfig}

final case class StravaConfig(
    credentials: StravaAppCredentials,
    clientConfig: StravaClientConfig,
    timeout: Duration
)
