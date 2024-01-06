package keeper.strava.data

import io.bullet.borer.NullOptions.*
import io.bullet.borer._
import io.bullet.borer.derivation.MapBasedCodecs._

final case class StravaAthlete(
    id: StravaAthleteId,
    username: Option[String],
    bikes: List[StravaGear],
    shoes: List[StravaGear]
)

object StravaAthlete {
  given Decoder[StravaAthlete] = deriveDecoder
}
