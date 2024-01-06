package keeper.bikes.event

import keeper.core.ComponentId

import io.bullet.borer.NullOptions.*
import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.{Decoder, Encoder}
import monocle.Lens

final case class ConfiguredFrontWheel(
    id: ComponentId,
    brakeDisc: Option[ComponentId] = None,
    tire: Option[ComponentId] = None,
    tube: Option[ComponentId] = None
)

object ConfiguredFrontWheel:
  given Encoder[ConfiguredFrontWheel] = deriveEncoder
  given Decoder[ConfiguredFrontWheel] = deriveDecoder

  val id: Lens[ConfiguredFrontWheel, ComponentId] =
    Lens[ConfiguredFrontWheel, ComponentId](_.id)(a => _.copy(id = a))

  val brakeDisc: Lens[ConfiguredFrontWheel, Option[ComponentId]] =
    Lens[ConfiguredFrontWheel, Option[ComponentId]](_.brakeDisc)(a =>
      _.copy(brakeDisc = a)
    )

  val tire: Lens[ConfiguredFrontWheel, Option[ComponentId]] =
    Lens[ConfiguredFrontWheel, Option[ComponentId]](_.tire)(a => _.copy(tire = a))

  val tube: Lens[ConfiguredFrontWheel, Option[ComponentId]] =
    Lens[ConfiguredFrontWheel, Option[ComponentId]](_.tube)(a => _.copy(tube = a))
