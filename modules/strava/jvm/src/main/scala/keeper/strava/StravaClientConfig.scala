package keeper.strava

import com.github.eikek.borer.compats.http4s.Http4sCodec
import io.bullet.borer.*
import io.bullet.borer.derivation.MapBasedCodecs.*
import org.http4s.Uri
import org.http4s.implicits.*

final case class StravaClientConfig(
    authUrl: Uri,
    tokenUri: Uri,
    apiUrl: Uri
)

object StravaClientConfig extends Http4sCodec {
  object Defaults {
    val authUrl = uri"https://www.strava.com/oauth/authorize"
    val apiUrl = uri"https://www.strava.com/api/v3"
    val tokenUrl = apiUrl / "oauth" / "token"
  }

  val default =
    StravaClientConfig(
      Defaults.authUrl,
      Defaults.tokenUrl,
      Defaults.apiUrl
    )

  given Encoder[StravaClientConfig] = deriveEncoder
}
