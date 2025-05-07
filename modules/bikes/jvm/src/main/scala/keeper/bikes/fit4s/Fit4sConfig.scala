package keeper.bikes.fit4s

import scala.concurrent.duration.*

import keeper.common.borer.BaseCodec.given

import com.github.eikek.borer.compats.http4s.Http4sCodec.given
import io.bullet.borer.Encoder
import io.bullet.borer.derivation.MapBasedCodecs.deriveEncoder
import org.http4s.Uri

final case class Fit4sConfig(
    baseUrl: Uri,
    timeout: Duration
)

object Fit4sConfig:
  given Encoder[Fit4sConfig] = deriveEncoder
