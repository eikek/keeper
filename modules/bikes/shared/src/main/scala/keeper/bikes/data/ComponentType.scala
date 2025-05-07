package keeper.bikes.data

import cats.{Eq, Show}

import keeper.common.borer.syntax.all.*

import io.bullet.borer.{Decoder, Encoder}

enum ComponentType:
  case Handlebar
  case Seatpost
  case Saddle
  case Stem
  case FrontWheel
  case RearWheel
  case Cassette
  case Chain
  case BrakeDisc
  case Tire
  case BrakePad
  case FrontBrake
  case RearBrake
  case Fork
  case FrontDerailleur
  case RearDerailleur
  case FrontMudguard
  case RearMudguard
  case InnerTube
  case CrankSet

  def name: String = productPrefix

object ComponentType:
  def fromString(s: String): Either[String, ComponentType] =
    ComponentType.values
      .find(_.productPrefix.equalsIgnoreCase(s))
      .toRight(s"Invalid component type: $s")

  given Encoder[ComponentType] = Encoder.forString.contramap(_.productPrefix.toLowerCase)
  given Decoder[ComponentType] = Decoder.forString.emap(fromString)
  given Eq[ComponentType] = Eq.fromUniversalEquals
  given Show[ComponentType] = Show.show(_.name)
