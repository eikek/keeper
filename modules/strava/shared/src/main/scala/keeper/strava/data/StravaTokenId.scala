package keeper.strava.data

import cats.Show

import io.bullet.borer.*

final class StravaTokenId(val id: Long) extends AnyVal {
  override def toString = s"StravaTokenId($id)"
}

object StravaTokenId {
  def apply(id: Long): StravaTokenId = new StravaTokenId(id)

  given Encoder[StravaTokenId] = Encoder.forLong.contramap(_.id)
  given Decoder[StravaTokenId] = Decoder.forLong.map(apply)

  given Show[StravaTokenId] = Show.show(_.id.toString)
}
