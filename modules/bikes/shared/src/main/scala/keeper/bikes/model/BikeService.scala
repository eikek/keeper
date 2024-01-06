package keeper.bikes.model

import java.time.Instant

import cats.Eq

import keeper.bikes.event.ServiceEvent
import keeper.common.borer.BaseCodec.given

import io.bullet.borer.NullOptions.*
import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.{Decoder, Encoder}

final case class BikeService(
    name: String,
    description: Option[String],
    date: Instant,
    createdAt: Option[Instant],
    totals: List[BikeTotal],
    events: List[ServiceEvent]
)

object BikeService:
  given Encoder[BikeService] = deriveEncoder
  given Decoder[BikeService] = deriveDecoder
  given Eq[BikeService] = Eq.fromUniversalEquals
