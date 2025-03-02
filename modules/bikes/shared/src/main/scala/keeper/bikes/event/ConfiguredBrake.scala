package keeper.bikes.event

import keeper.core.ComponentId

import io.bullet.borer.NullOptions.given
import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.{Decoder, Encoder}
import monocle.Lens

final case class ConfiguredBrake(
    id: ComponentId,
    pad: Option[ComponentId] = None
)

object ConfiguredBrake:
  given Encoder[ConfiguredBrake] = deriveEncoder
  given Decoder[ConfiguredBrake] = deriveDecoder

  val id: Lens[ConfiguredBrake, ComponentId] =
    Lens[ConfiguredBrake, ComponentId](_.id)(a => _.copy(id = a))

  val pad: Lens[ConfiguredBrake, Option[ComponentId]] =
    Lens[ConfiguredBrake, Option[ComponentId]](_.pad)(a => _.copy(pad = a))
