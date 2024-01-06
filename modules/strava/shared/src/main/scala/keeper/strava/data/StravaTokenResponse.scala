package keeper.strava.data

import java.time.{Duration, Instant}

import io.bullet.borer.*
import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.derivation.key

final case class StravaTokenResponse(
    @key("token_type") tokenType: String,
    @key("access_token") accessToken: StravaAccessToken,
    @key("expires_at") expiresAt: Instant,
    @key("expires_in") expiresIn: Duration,
    @key("refresh_token") refreshToken: StravaRefreshToken
)

object StravaTokenResponse {

  implicit val jsonDecoder: Decoder[StravaTokenResponse] =
    JsonCodec.decoder

  implicit val jsonEncoder: Encoder[StravaTokenResponse] =
    JsonCodec.encoder

  private object JsonCodec {
    implicit val instantDecoder: Decoder[Instant] =
      Decoder.forLong.map(Instant.ofEpochSecond)

    implicit val instantEncoder: Encoder[Instant] =
      Encoder.forLong.contramap(_.getEpochSecond)

    implicit val durationDecoder: Decoder[Duration] =
      Decoder.forLong.map(Duration.ofSeconds)

    implicit val durationEncoder: Encoder[Duration] =
      Encoder.forLong.contramap(_.toSeconds)

    val decoder: Decoder[StravaTokenResponse] =
      deriveDecoder

    val encoder: Encoder[StravaTokenResponse] =
      deriveEncoder
  }
}
