package keeper.common

import cats.data.NonEmptyList
import cats.{Eq, Show}
import fs2.{Pure, Stream}

import keeper.common.Node.Path

final case class Node[A](value: A, children: Seq[Node[A]]):
  def find[B: Eq](path: Path[B], select: A => B): Option[Node[A]] =
    path.uncons match
      case Some((h, Path.Empty)) =>
        if (Eq.eqv(select(value), h)) Some(this)
        else None

      case Some((h, tail)) =>
        if (Eq.eqv(select(value), h)) children.flatMap(_.find(tail, select)).headOption
        else None

      case None =>
        None

  def paths[B](select: A => B): Seq[Path[B]] =
    if (children.isEmpty) Seq(Path(select(value)))
    else children.flatMap(_.paths(select)).map(p => select(value) :: p)

  def mapValues[B](f: A => B): Node[B] =
    Node(f(value), children.map(_.mapValues(f)))

  def traverse: Stream[Pure, Node[A]] =
    Stream.emit(this) ++ Stream.emits(children).flatMap(_.traverse)

object Node:
  given nodeShow[A: Show]: Show[Node[A]] = Show.show { n =>
    val tail =
      if (n.children.isEmpty) ""
      else ", " + n.children.map(_.toString).mkString("[", ", ", "]")
    s"Node(${n.value}$tail)"
  }

  def of[A](n: A, children: Node[A]*): Node[A] =
    Node(n, children)

  sealed trait Path[+A]:
    def isEmpty: Boolean
    def map[B](f: A => B): Path[B]
    def uncons: Option[(A, Path[A])]
    def ::[B >: A](v: B): Path[B] = this match
      case Path.Empty => Path(v)
      case Path.NonEmpty(ps) =>
        Path.NonEmpty(NonEmptyList(v, ps.toList))

    def isPrefixOf[B >: A](other: Path[B]): Boolean
    def toList: List[A]

  object Path:
    case object Empty extends Path[Nothing] {
      val isEmpty: Boolean = true
      val uncons = None
      def map[B](f: Nothing => B): Path[B] = this
      def isPrefixOf[B >: Nothing](other: Path[B]): Boolean = false
      def toList = Nil
      override def toString: String = "nil"
    }
    final case class NonEmpty[+A](nodes: NonEmptyList[A]) extends Path[A] {
      val isEmpty: Boolean = false
      val uncons: Option[(A, Path[A])] =
        Some((nodes.head, fromList(nodes.tail)))

      def isPrefixOf[B >: A](other: Path[B]): Boolean =
        other match
          case Empty => false
          case NonEmpty(on) =>
            if (nodes.length > on.length) false
            else nodes.zip(on).forall(_ == _)

      def map[B](f: A => B): Path[B] = NonEmpty(nodes.map(f))
      def toList: List[A] = nodes.toList
      override def toString: String =
        nodes.map(_.toString).toList.mkString("[", ", ", "]")
    }

    def fromList[A](ns: List[A]): Path[A] =
      NonEmptyList.fromList(ns).map(NonEmpty.apply).getOrElse(empty)

    def empty[A]: Path[A] = Empty
    def apply[A](n: A, ns: A*): Path[A] =
      NonEmpty(NonEmptyList(n, ns.toList))
