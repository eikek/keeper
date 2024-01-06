package keeper.strava.data

import io.bullet.borer._
import io.bullet.borer.derivation.MapBasedCodecs._
import org.http4s.Uri

final class StravaActivityId(val id: Long) extends AnyVal {
  override def toString = s"StravaActivity($id)"
}

object StravaActivityId {
  def apply(id: Long): StravaActivityId = new StravaActivityId(id)

  given Decoder[StravaActivityId] = Decoder.forLong.map(StravaActivityId.apply)
  given Encoder[StravaActivityId] = Encoder.forLong.contramap(_.id)

  given Uri.Path.SegmentEncoder[StravaActivityId] =
    Uri.Path.SegmentEncoder.longSegmentEncoder.contramap(_.id)
}
