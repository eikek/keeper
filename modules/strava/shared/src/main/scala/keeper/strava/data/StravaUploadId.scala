package keeper.strava.data

import io.bullet.borer._
import org.http4s.Uri

final class StravaUploadId(val id: Long) extends AnyVal {
  override def toString = s"StravaUploadId($id)"
}

object StravaUploadId {
  def apply(id: Long): StravaUploadId = new StravaUploadId(id)

  given Decoder[StravaUploadId] = Decoder.forLong.map(StravaUploadId.apply)
  given Encoder[StravaUploadId] = Encoder.forLong.contramap(_.id)

  given Uri.Path.SegmentEncoder[StravaUploadId] =
    Uri.Path.SegmentEncoder.longSegmentEncoder.contramap(_.id)
}
