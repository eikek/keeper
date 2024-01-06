package keeper.common

import cats.effect.IO
import cats.{Applicative, Eq, Monoid}
import fs2.concurrent.SignallingRef

import monocle.{Lens, Traversal}

object Lenses {

  def noop[A, B](defval: B): Lens[A, B] =
    Lens[A, B](_ => defval)(_ => identity)

  /** Returns an empty A for a None. Sets a Some or an empty A for a given None. */
  def optionToEmpty[A: Monoid: Eq]: Lens[Option[A], A] =
    Lens[Option[A], A](_.getOrElse(Monoid[A].empty))(a =>
      _ => Option(a).filterNot(Monoid[A].isEmpty)
    )

  val emptyString: Lens[Option[String], String] =
    optionToEmpty[String]

  /** Like optionToEmpty, but keeps the existing value on set, if a None is supplied */
  def optionSet[A]: Lens[A, Option[A]] =
    Lens[A, Option[A]](Some(_))(a => b => a.getOrElse(b))

  def mapValues[K, V]: Traversal[Map[K, V], V] = monocle.function.all.each[Map[K, V], V]

  def mapKeys[K, V]: Traversal[Map[K, V], K] =
    new Traversal[Map[K, V], K]:
      def modifyA[F[_]: Applicative](f: K => F[K])(s: Map[K, V]): F[Map[K, V]] =
        s.foldLeft(Applicative[F].pure(Map.empty[K, V])) { case (acc, (k, v)) =>
          Applicative[F].map2(f(k), acc)((head, tail) => tail + (head -> v))
        }

  object syntax:
    extension [A](ref: SignallingRef[IO, A])
      def to[B](fm: Lens[A, B]): SignallingRef[IO, B] =
        SignallingRef.lens(ref)(fm.get, a => b => fm.replace(b)(a))
}
