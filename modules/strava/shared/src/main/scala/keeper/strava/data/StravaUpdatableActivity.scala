package keeper.strava.data

import io.bullet.borer.NullOptions.given
import io.bullet.borer.*
import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.derivation.key

final case class StravaUpdatableActivity(
    commute: Option[Boolean] = None,
    trainer: Option[Boolean] = None,
    description: Option[String] = None,
    name: Option[String] = None,
    @key("gear_id") gearId: Option[String] = None
) {
  def isEmpty: Boolean =
    commute.isEmpty && trainer.isEmpty && description.isEmpty &&
      name.isEmpty && gearId.isEmpty
}

object StravaUpdatableActivity {
  val empty: StravaUpdatableActivity =
    StravaUpdatableActivity(None, None, None, None, None)

  given Encoder[StravaUpdatableActivity] = deriveEncoder
  given Decoder[StravaUpdatableActivity] = deriveDecoder
}
