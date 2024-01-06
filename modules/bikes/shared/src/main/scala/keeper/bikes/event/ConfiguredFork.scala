package keeper.bikes.event

import keeper.core.ComponentId

import io.bullet.borer.NullOptions.*
import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.{Decoder, Encoder}
import monocle.Lens

final case class ConfiguredFork(
    id: ComponentId,
    brake: Option[ConfiguredBrake] = None,
    mudguard: Option[ComponentId] = None
)

object ConfiguredFork:
  given Encoder[ConfiguredFork] = deriveEncoder
  given Decoder[ConfiguredFork] = deriveDecoder

  val id: Lens[ConfiguredFork, ComponentId] =
    Lens[ConfiguredFork, ComponentId](_.id)(a => _.copy(id = a))

  val brake: Lens[ConfiguredFork, Option[ConfiguredBrake]] =
    Lens[ConfiguredFork, Option[ConfiguredBrake]](_.brake)(a => _.copy(brake = a))

  val mudguard: Lens[ConfiguredFork, Option[ComponentId]] =
    Lens[ConfiguredFork, Option[ComponentId]](_.mudguard)(a => _.copy(mudguard = a))
