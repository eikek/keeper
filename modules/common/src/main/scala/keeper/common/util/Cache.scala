package keeper.common.util

trait Cache[F[_], A, B] {

  def cached(f: A => F[Option[B]]): A => F[Option[B]]

}

object Cache {
  def noop[F[_], A, B]: Cache[F, A, B] =
    (f: A => F[Option[B]]) => f

}
