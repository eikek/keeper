package keeper.client.data

import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.{Decoder, Encoder}

final case class CreateResult(
    id: Long
)
object CreateResult:
  def apply(n: Int): CreateResult = CreateResult(n.toLong)

  given Encoder[CreateResult] = deriveEncoder
  given Decoder[CreateResult] = deriveDecoder
