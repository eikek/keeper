package keeper.bikes.event

import cats.{Eq, Functor}

import keeper.bikes.event.Alter.{Discard, Unset}

import io.bullet.borer.{Decoder, Encoder}

enum Alter[+A]:
  case Discard extends Alter[Nothing]
  case Unset extends Alter[Nothing]
  case Replace(value: A) extends Alter[A]

  def isDiscard: Boolean = this == Discard
  def isUnset: Boolean = this == Unset
  def isReplace[B >: A](value: B): Boolean = this == Replace(value)

  def asOption: Option[A] = this match
    case Discard    => None
    case Unset      => None
    case Replace(a) => Some(a)

  def apply[B >: A](v: Option[B]) = this match
    case Discard    => v
    case Unset      => None
    case Replace(a) => Some(a)

  def fold[B](discard: => B, unset: => B, replace: A => B): B = this match
    case Discard    => discard
    case Unset      => unset
    case Replace(a) => replace(a)

object Alter:
  given alterEq[A]: Eq[Alter[A]] = Eq.fromUniversalEquals
  given jsonEncoder[A: Encoder]: Encoder[Alter[A]] =
    Encoder[Option[Option[A]]].contramap {
      case Alter.Discard    => None
      case Alter.Unset      => Some(None)
      case Alter.Replace(a) => Some(Some(a))
    }

  given jsonDecoder[A: Decoder]: Decoder[Alter[A]] =
    Decoder[Option[Option[A]]].map {
      case None          => Alter.Discard
      case Some(None)    => Alter.Unset
      case Some(Some(a)) => Alter.Replace(a)
    }

  given Functor[Alter] =
    new Functor[Alter]:
      def map[A, B](fa: Alter[A])(f: A => B): Alter[B] =
        fa match
          case Alter.Discard    => Alter.Discard
          case Alter.Unset      => Alter.Unset
          case Alter.Replace(a) => Alter.Replace(f(a))
