package keeper.server.util

import java.time.Instant

import cats.data.ValidatedNel
import cats.effect.Sync
import cats.effect.kernel.Clock
import cats.syntax.all.*
import cats.{Applicative, Monad}

import keeper.client.data.RequestFailure

import com.github.eikek.borer.compats.http4s.BorerEntityJsonCodec
import org.http4s.*
import org.http4s.dsl.Http4sDsl

trait MoreHttp4sDsl[F[_]: Sync] extends BorerEntityJsonCodec { self: Http4sDsl[F] =>

  implicit final class ValidatedToResponse(
      resp: ValidatedNel[ParseFailure, F[Response[F]]]
  )(implicit F: Monad[F]) {
    def orBadRequest =
      resp.fold(errs => BadRequest(RequestFailure(errs)), identity)
  }

  implicit final class OptionToResponse[A](resp: Option[A])(implicit
      e: EntityEncoder[F, A],
      F: Applicative[F]
  ) {
    def orNotFound(msg: String) =
      resp.fold(NotFoundF(msg))(Ok(_))
  }

  extension (self: Option[ValidatedNel[ParseFailure, Instant]])
    def withValid[A](f: Instant => F[A]): ValidatedNel[ParseFailure, F[A]] =
      self match
        case None    => Clock[F].realTimeInstant.flatMap(f).validNel
        case Some(v) => v.map(f)

  def InternalServerError(ex: Throwable): Response[F] =
    ErrorResponse(ex)

  def NotFound(msg: String): Response[F] =
    ErrorResponse(Status.NotFound, msg)

  def NotFoundF(msg: String)(implicit F: Applicative[F]): F[Response[F]] =
    NotFound(msg).pure[F]
}
