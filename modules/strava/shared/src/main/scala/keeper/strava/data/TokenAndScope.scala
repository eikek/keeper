package keeper.strava.data

import io.bullet.borer.*
import io.bullet.borer.derivation.MapBasedCodecs.*

final case class TokenAndScope(
    tokenResponse: StravaTokenResponse,
    scope: StravaScope
):
  val accessToken: StravaAccessToken = tokenResponse.accessToken

object TokenAndScope {
  given Decoder[TokenAndScope] = deriveDecoder
  given Encoder[TokenAndScope] = deriveEncoder
}
