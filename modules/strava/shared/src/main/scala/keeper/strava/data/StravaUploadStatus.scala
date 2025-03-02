package keeper.strava.data

import io.bullet.borer.*
import io.bullet.borer.NullOptions.given
import io.bullet.borer.derivation.MapBasedCodecs.*

final case class StravaUploadStatus(
    id: StravaUploadId,
    external_id: Option[String],
    error: Option[String],
    status: String,
    activity_id: Option[StravaActivityId]
)

object StravaUploadStatus {
  given Decoder[StravaUploadStatus] = deriveDecoder
  given Encoder[StravaUploadStatus] = deriveEncoder
}
