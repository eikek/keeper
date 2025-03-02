package keeper.strava.data

import io.bullet.borer.*

final class StravaAccessToken(val token: String) extends AnyVal {
  override def toString = "StravaAccessToken(***)"
}

object StravaAccessToken {
  def apply(token: String): StravaAccessToken = new StravaAccessToken(token)

  given Encoder[StravaAccessToken] = Encoder.forString.contramap(_.token)
  given Decoder[StravaAccessToken] = Decoder.forString.map(apply)
}
