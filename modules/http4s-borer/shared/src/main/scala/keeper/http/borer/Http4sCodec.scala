package keeper.http.borer

import io.bullet.borer.*
import org.http4s.*

trait Http4sCodec:
  given Encoder[Uri] = Encoder.forString.contramap(_.renderString)

object Http4sCodec extends Http4sCodec
