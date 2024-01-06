package keeper.bikes

import keeper.bikes.db.PostgresConfig
import keeper.bikes.fit4s.Fit4sConfig
import keeper.bikes.strava.StravaConfig

final case class Config(
    database: PostgresConfig,
    fit4sConfig: Option[Fit4sConfig],
    stravaConfig: Option[StravaConfig]
)
