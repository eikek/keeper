package keeper.bikes.data

import keeper.common.borer.syntax.all.*

import io.bullet.borer.{Decoder, Encoder}

enum ComponentState:
  case Active
  case Inactive

  def name: String = productPrefix

object ComponentState:
  def fromString(str: String): Either[String, ComponentState] =
    values
      .find(_.name.equalsIgnoreCase(str))
      .toRight(s"Invalid part state name: $str")

  given Encoder[ComponentState] = Encoder.forString.contramap(_.name)
  given Decoder[ComponentState] = Decoder.forString.emap(fromString)
