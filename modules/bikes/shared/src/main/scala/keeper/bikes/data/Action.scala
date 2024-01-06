package keeper.bikes.data

import cats.syntax.all.*

import io.bullet.borer.NullOptions.*
import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.{Decoder, Encoder}

final case class Action(
    id: ActionId,
    name: ActionName,
    description: Option[String]
)

object Action:
  given Decoder[Action] = deriveDecoder
  given Encoder[Action] = deriveEncoder

  val add: Action = Action(
    id = ActionId(1L),
    name = ActionName.Add,
    description = "Add component to device or sub-component to component".some
  )

  val remove: Action = Action(
    id = ActionId(2L),
    name = ActionName.Remove,
    description =
      "Remove a component from a device or a sub-component from a component".some
  )
