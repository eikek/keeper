package keeper.bikes.data

import java.time.Instant

import keeper.common.borer.BaseCodec.given
import keeper.core.DeviceId

import io.bullet.borer.NullOptions.*
import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.{Decoder, Encoder}

final case class Device(
    id: DeviceId,
    brandId: BrandId,
    name: String,
    description: Option[String],
    state: ComponentState,
    addedAt: Instant,
    removedAt: Option[Instant],
    createdAt: Instant
)

object Device:
  given Encoder[Device] = deriveEncoder
  given Decoder[Device] = deriveDecoder
