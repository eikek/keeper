package keeper.strava.data

import java.time.Instant

import cats.syntax.all.*

import keeper.common.borer.syntax.all.*

import io.bullet.borer.NullOptions._
import io.bullet.borer._
import io.bullet.borer.derivation.MapBasedCodecs._

final case class StravaActivity(
    name: String,
    sport_type: String,
    id: StravaActivityId,
    start_date: Instant,
    trainer: Boolean,
    commute: Boolean,
    gear_id: Option[String],
    external_id: String
) {}

object StravaActivity {
  given Decoder[StravaActivity] = JsonCodec.activityDecoder

  private object JsonCodec {

    given Decoder[Instant] =
      Decoder.forString.emap(s =>
        Either.catchNonFatal(Instant.parse(s)).leftMap(_.getMessage)
      )

    val activityDecoder: Decoder[StravaActivity] =
      deriveDecoder
  }
}
