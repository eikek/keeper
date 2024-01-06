package keeper.bikes.fit4s

import io.bullet.borer.Decoder
import io.bullet.borer.derivation.MapBasedCodecs.deriveDecoder

final case class Fit4sSummary(
    distance: Fit4sSummary.Distance
)
object Fit4sSummary:
  given Decoder[Fit4sSummary] = deriveDecoder

  final case class Distance(
      label: String,
      meter: Double
  )
  object Distance:
    given Decoder[Distance] = deriveDecoder
