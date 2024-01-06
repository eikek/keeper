package keeper.strava

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.syntax.all._

import keeper.strava.data.StravaAccessToken

import org.http4s.Status.Successful
import org.http4s._
import org.http4s.client.Client
import org.http4s.headers.{Accept, Authorization, MediaRangeAndQValue}

package object impl {

  implicit final class RequestOps[F[_]](val self: Request[F]) {
    def putAuth(accessToken: StravaAccessToken) =
      self.putHeaders(
        Authorization(Credentials.Token(AuthScheme.Bearer, accessToken.token))
      )
  }

  implicit final class HttpClientOps[F[_]: Sync](val self: Client[F]) {

    def expectEither[B, A](
        req: Request[F]
    )(implicit da: EntityDecoder[F, A], db: EntityDecoder[F, B]): F[Either[B, A]] = {
      val r = if (da.consumes.nonEmpty) {
        val m = da.consumes.toList.map(MediaRangeAndQValue(_))
        req.addHeader(Accept(NonEmptyList.fromListUnsafe(m)))
      } else req

      self.run(r).use {
        case Successful(resp) =>
          da.decode(resp, strict = false)
            .leftWiden[Throwable]
            .rethrowT
            .map(_.asRight[B])
        case resp =>
          db.decode(resp, strict = false)
            .leftWiden[Throwable]
            .rethrowT
            .map(_.asLeft[A])
      }
    }
  }
}
