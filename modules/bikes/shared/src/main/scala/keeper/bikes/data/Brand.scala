package keeper.bikes.data

import java.time.Instant

import cats.Eq

import keeper.common.borer.BaseCodec.given

import io.bullet.borer.NullOptions.*
import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.{Decoder, Encoder}

final case class Brand(
    id: BrandId,
    name: String,
    description: Option[String],
    createdAt: Instant
)

object Brand:
  given Encoder[Brand] = deriveEncoder
  given Decoder[Brand] = deriveDecoder

  given Eq[Brand] = Eq.fromUniversalEquals
