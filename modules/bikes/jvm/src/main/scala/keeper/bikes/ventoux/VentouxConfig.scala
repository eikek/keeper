package keeper.bikes.ventoux

import java.time.ZoneId

import scala.concurrent.duration.*

import keeper.common.borer.BaseCodec.given

import com.github.eikek.borer.compats.http4s.Http4sCodec.given
import io.bullet.borer.Encoder
import io.bullet.borer.derivation.MapBasedCodecs.deriveEncoder
import org.http4s.Uri

final case class VentouxConfig(
    baseUrl: Uri,
    apiKey: String,
    timeout: Duration,
    timezone: ZoneId
)

object VentouxConfig:
  given Encoder[VentouxConfig] = deriveEncoder
