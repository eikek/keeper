package keeper.bikes.event

import keeper.core.ComponentId

import io.bullet.borer.NullOptions.given
import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.{Decoder, Encoder}
import monocle.Lens

final case class ConfiguredRearWheel(
    id: ComponentId,
    brakeDisc: Option[ComponentId] = None,
    tire: Option[ComponentId] = None,
    cassette: Option[ComponentId] = None,
    tube: Option[ComponentId] = None
)

object ConfiguredRearWheel:
  given Encoder[ConfiguredRearWheel] = deriveEncoder
  given Decoder[ConfiguredRearWheel] = deriveDecoder

  val id: Lens[ConfiguredRearWheel, ComponentId] =
    Lens[ConfiguredRearWheel, ComponentId](_.id)(a => _.copy(id = a))

  val brakeDisc: Lens[ConfiguredRearWheel, Option[ComponentId]] =
    Lens[ConfiguredRearWheel, Option[ComponentId]](_.brakeDisc)(a =>
      _.copy(brakeDisc = a)
    )

  val tire: Lens[ConfiguredRearWheel, Option[ComponentId]] =
    Lens[ConfiguredRearWheel, Option[ComponentId]](_.tire)(a => _.copy(tire = a))

  val cassette: Lens[ConfiguredRearWheel, Option[ComponentId]] =
    Lens[ConfiguredRearWheel, Option[ComponentId]](_.cassette)(a => _.copy(cassette = a))

  val tube: Lens[ConfiguredRearWheel, Option[ComponentId]] =
    Lens[ConfiguredRearWheel, Option[ComponentId]](_.tube)(a => _.copy(tube = a))
