package keeper.client.data

import cats.{Applicative, Functor}

enum FetchResult[+A]:
  case Success(value: A)
  case RequestFailed(failures: RequestFailure)

  def map[B](f: A => B): FetchResult[B] = this match
    case Success(a) => Success(f(a))
    case _          => this.asInstanceOf[FetchResult[B]]

  def fold[B](fa: A => B, fb: RequestFailure => B): B = this match
    case Success(a)       => fa(a)
    case RequestFailed(b) => fb(b)

  def toEither: Either[RequestFailure, A] =
    fold(Right(_), Left(_))
end FetchResult

object FetchResult {
  given Applicative[FetchResult] =
    new Applicative[FetchResult] {
      def pure[A](a: A): FetchResult[A] = FetchResult.Success(a)
      def ap[A, B](ff: FetchResult[A => B])(fa: FetchResult[A]): FetchResult[B] =
        ff match {
          case FetchResult.Success(f) =>
            fa match {
              case FetchResult.Success(a) => FetchResult.Success(f(a))
              case err                    => err.asInstanceOf[FetchResult[B]]
            }
          case err => err.asInstanceOf[FetchResult[B]]
        }
    }

  given Functor[FetchResult] =
    new Functor[FetchResult] {
      def map[A, B](fa: FetchResult[A])(f: A => B): FetchResult[B] =
        fa.map(f)
    }
}
