package keeper.client.data

import cats.Eq

import io.bullet.borer.derivation.MapBasedCodecs.{deriveDecoder, deriveEncoder}
import io.bullet.borer.{Decoder, Encoder}

final case class StravaConnectState(
    enabled: Boolean,
    valid: Boolean
)

object StravaConnectState:
  given Encoder[StravaConnectState] = deriveEncoder
  given Decoder[StravaConnectState] = deriveDecoder
  given Eq[StravaConnectState] = Eq.fromUniversalEquals
