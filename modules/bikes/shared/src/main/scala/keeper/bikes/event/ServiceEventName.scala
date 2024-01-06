package keeper.bikes.event

import cats.Eq

import keeper.common.borer.syntax.all.*
import keeper.common.util.StringCase

import io.bullet.borer.{Decoder, Encoder}

enum ServiceEventName:
  case NewBike
  case ChangeBike
  case ChangeFrontWheel
  case ChangeRearWheel
  case ChangeFork
  case ChangeTires
  case ChangeBrakePads
  case WaxChain
  case CeaseComponent
  case CeaseBike
  case PatchTube
  case PatchTire
  case CleanComponent
  case CleanBike

  def name: String = this.productPrefix.toLowerCase
  def label: String = StringCase.camelToSpace(productPrefix)

object ServiceEventName:
  given Eq[ServiceEventName] = Eq.fromUniversalEquals
  given Encoder[ServiceEventName] = Encoder.forString.contramap(_.name)
  given Decoder[ServiceEventName] = Decoder.forString.emap(fromName)

  def fromName(str: String): Either[String, ServiceEventName] =
    ServiceEventName.values
      .find(_.name.equalsIgnoreCase(str))
      .toRight(s"Invalid service event name: $str")
