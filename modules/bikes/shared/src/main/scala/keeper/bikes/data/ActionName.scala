package keeper.bikes.data

import cats.Order

import keeper.bikes.data.ActionName.{DripWax, HotWax}
import keeper.common.borer.syntax.all.*

import io.bullet.borer.{Decoder, Encoder}

enum ActionName:
  case Remove
  case Add
  case Drop
  case HotWax
  case DripWax
  case Cease
  case Patch
  case Clean

  def asString: String = this match
    case Add     => "add"
    case Remove  => "remove"
    case Drop    => "drop"
    case HotWax  => "hotwax"
    case DripWax => "dripwax"
    case Cease   => "cease"
    case Patch   => "patch"
    case Clean   => "clean"

  def fold0[A](add: => A, remove: => A, drop: => A): Option[A] = this match
    case Add    => Some(add)
    case Remove => Some(remove)
    case Drop   => Some(drop)
    case _      => None

  def isAddRemoveOrCease: Boolean = ActionName.addRemCease.contains(this)

object ActionName:
  private val addRemCease = Set(Add, Remove, Cease)

  def fromString(str: String): Either[String, ActionName] =
    ActionName.values
      .find(_.productPrefix.equalsIgnoreCase(str))
      .toRight(s"Invalid action name: $str")

  object AddRemoveOrCease {
    def unapply(a: ActionName): Option[ActionName] =
      if (a.isAddRemoveOrCease) Some(a) else None
  }

  given Encoder[ActionName] = Encoder.forString.contramap(_.productPrefix.toLowerCase)
  given Decoder[ActionName] = Decoder.forString.emap(fromString)
  given Order[ActionName] = Order.by(_.ordinal)
