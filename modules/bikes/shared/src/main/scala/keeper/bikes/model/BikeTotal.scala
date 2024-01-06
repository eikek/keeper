package keeper.bikes.model

import cats.Show
import cats.syntax.show.*

import keeper.common.Distance
import keeper.core.DeviceId

import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.{Decoder, Encoder}

final case class BikeTotal(
    bikeId: DeviceId,
    distance: Distance
)

object BikeTotal:
  given Encoder[BikeTotal] = deriveEncoder
  given Decoder[BikeTotal] = deriveDecoder
  given Show[BikeTotal] = Show.show(e => s"${e.bikeId}: ${e.distance.show}")

  def zero(bikeId: DeviceId): BikeTotal = BikeTotal(bikeId, Distance.zero)
