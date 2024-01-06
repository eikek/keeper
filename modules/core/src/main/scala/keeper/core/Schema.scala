package keeper.core

import cats.syntax.all.*
import cats.{Eq, Show}

import keeper.common.Node
import keeper.common.Node.Path
import keeper.core.Schema.Value

final case class Schema[A: Eq](topLevel: Seq[Node[Value[A]]]):
  def findTopLevel(ct: A): Option[Node[Value[A]]] =
    find(Path(ct))

  def find(path: Path[A]): Option[Node[Value[A]]] =
    topLevel.collectFirst(Function.unlift(n => n.find(path, _.ctype)))

  def paths: Seq[Path[A]] =
    topLevel.flatMap(_.paths(_.ctype))

object Schema:
  final case class Value[A](ctype: A, maxOccurrence: Int)

  object Value:
    given valueShow[A: Show]: Show[Value[A]] =
      Show.show(v => show"${v.ctype}[max=${v.maxOccurrence}]")

  def of[A: Eq](nodes: Node[Value[A]]*): Schema[A] = Schema(nodes.toList)
