package keeper.strava.data

import io.bullet.borer.*

final class StravaRefreshToken(val token: String) extends AnyVal {
  override def toString = "StravaRefreshToken(***)"
}

object StravaRefreshToken {
  def apply(token: String): StravaRefreshToken = new StravaRefreshToken(token)

  given Encoder[StravaRefreshToken] = Encoder.forString.contramap(_.token)
  given Decoder[StravaRefreshToken] = Decoder.forString.map(apply)
}
