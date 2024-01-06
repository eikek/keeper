package keeper.core

import cats.{Eq, Show}

enum ComponentType:
  case FrontWheel
  case RearWheel
  case Chain
  case Cassette
  case Tire
  case FrontBrake
  case RearBrake
  case Seatpost
  case BrakePad
  case BrakeDisc
  case Fork

object ComponentType:
  given Show[ComponentType] = Show.fromToString
  given Eq[ComponentType] = Eq.fromUniversalEquals
