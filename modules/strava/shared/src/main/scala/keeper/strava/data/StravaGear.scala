package keeper.strava.data

import keeper.common.Distance

import io.bullet.borer.*
import io.bullet.borer.derivation.MapBasedCodecs.*

final case class StravaGear(
    id: String,
    primary: Boolean,
    distance: Distance,
    name: String
)

object StravaGear {
  given Decoder[StravaGear] = deriveDecoder
}
