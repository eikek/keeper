package keeper.strava.data

import io.bullet.borer._

final class StravaAthleteId(val id: Long) extends AnyVal {
  override def toString = s"StravaAthleteId($id)"
}

object StravaAthleteId {
  def apply(id: Long): StravaAthleteId = new StravaAthleteId(id)

  given Decoder[StravaAthleteId] = Decoder.forLong.map(apply)
}
