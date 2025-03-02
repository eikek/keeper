package keeper.strava.impl

import java.time.Instant

import scala.concurrent.duration.*
import scala.math.Ordering.Implicits.infixOrderingOps

import cats.data.EitherT
import cats.effect.{Clock, Sync}
import cats.syntax.all.*
import fs2.Stream

import keeper.strava.*
import keeper.strava.data._

import org.http4s.{Request, Uri}

final class DefaultStravaService[F[_]: Sync](
    cfg: StravaAppCredentials,
    client: StravaClient[F],
    tokenStore: TokenStore[F]
) extends StravaService[F] {
  private val logger = scribe.cats.effect[F]

  def createAuthUrl(
      redirectUrl: String,
      state: String,
      scope: StravaScope
  ): Uri =
    client.authRequestUrl(cfg, redirectUrl, state, scope)

  def resumeAuth(
      state: String,
      req: Request[F]
  ): F[Either[String, TokenAndScope]] =
    EitherT(client.resumeAuthRequest(cfg, state)(req))
      .semiflatMap(token => tokenStore.store(cfg.clientId, token).as(token))
      .value

  def initOAuth(timeout: FiniteDuration): F[Option[TokenAndScope]] =
    nonInteractiveOAuth.flatMap {
      case Some(t) => Option(t).pure[F]
      case None =>
        client
          .initAuth(cfg, timeout)
          .flatMap(storeTokenResponse)
    }

  def findToken: F[Option[TokenAndScope]] =
    tokenStore.findLatest(cfg.clientId)

  def nonInteractiveOAuth: F[Option[TokenAndScope]] =
    Clock[F].realTimeInstant.flatMap { now =>
      tokenStore.findLatest(cfg.clientId).flatMap {
        case Some(t) if t.tokenResponse.expiresAt.plusSeconds(20) > now =>
          logger.debug(s"Latest token is still valid.").as(Option(t))

        case Some(t) =>
          client
            .refreshAuth(cfg, t.pure[F])
            .map(_.some)
            .flatMap(storeTokenResponse)

        case None =>
          Option.empty.pure[F]
      }
    }

  private def storeTokenResponse(resp: Option[TokenAndScope]) =
    resp match {
      case Some(tr) =>
        logger.debug(s"Got token response, storing to db") *>
          tokenStore.store(cfg.clientId, tr).as(tr.some)
      case None =>
        Option.empty.pure[F]
    }

  private def requireToken =
    nonInteractiveOAuth
      .map(_.toRight(new Exception(s"No authentication token available.")))
      .rethrow

  def listActivities(
      after: Instant,
      before: Instant,
      page: Int,
      perPage: Int
  ): F[List[StravaActivity]] =
    requireToken.flatMap(token =>
      client.listActivities(
        token.accessToken,
        after,
        before,
        page,
        perPage
      )
    )

  def listAllActivities(
      after: Instant,
      before: Instant,
      chunkSize: Int
  ): Stream[F, StravaActivity] =
    Stream
      .eval(requireToken)
      .flatMap(token =>
        client.listAllActivities(token.accessToken, after, before, chunkSize)
      )

  def findGear(gearId: String): F[Option[StravaGear]] =
    requireToken.flatMap(token => client.findGear(token.accessToken, gearId))

  def getAthlete: F[StravaAthlete] =
    requireToken.flatMap(token => client.getAthlete(token.accessToken))
}
