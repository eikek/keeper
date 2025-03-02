package keeper.strava.data

import io.bullet.borer.*
import io.bullet.borer.NullOptions.given
import io.bullet.borer.derivation.MapBasedCodecs.*

final case class StravaAthlete(
    id: StravaAthleteId,
    username: Option[String],
    bikes: List[StravaGear],
    shoes: List[StravaGear]
)

object StravaAthlete {
  given Decoder[StravaAthlete] = deriveDecoder
}
