package keeper.core

import cats.Applicative
import cats.syntax.all.*

/** Associate type information (A) to components. */
trait TypeInfo[F[_], A]:
  def componentType(cid: ComponentId): F[Option[A]]
  def findByType(ct: A): F[Set[ComponentId]]

object TypeInfo:
  def fromList[F[_]: Applicative, A](types: List[(ComponentId, A)]): TypeInfo[F, A] =
    fromMap(types.toMap)

  def fromMap[F[_]: Applicative, A](typeMap: Map[ComponentId, A]): TypeInfo[F, A] =
    val typeMapRev = typeMap.groupBy(_._2).view.mapValues(_.keySet).toMap
    new TypeInfo[F, A]:
      def componentType(cid: ComponentId): F[Option[A]] = typeMap.get(cid).pure[F]
      def findByType(ct: A): F[Set[ComponentId]] =
        typeMapRev.getOrElse(ct, Set.empty).pure[F]
