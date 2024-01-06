package keeper.strava.impl

import java.net.ServerSocket

import scala.concurrent.duration.*

import cats.effect.*
import cats.effect.std.Random
import cats.syntax.all.*
import fs2.io.net.Network

import keeper.http.borer.BorerEntityCodec.Implicits.*
import keeper.strava.data.*
import keeper.strava.{StravaAppCredentials, StravaClientConfig, data}

import com.comcast.ip4s.{Host, Port}
import org.http4s.Method.POST
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import scodec.bits.ByteVector

final class StravaOAuth[F[_]: Async: Network](
    config: StravaClientConfig,
    client: Client[F]
) {

  private[this] val logger = scribe.cats[F]

  def authRequestUrl(
      cfg: StravaAppCredentials,
      redirectUrl: String,
      state: String,
      scope: StravaScope
  ): Uri =
    config.authUrl
      .withQueryParam("client_id", cfg.clientId)
      .withQueryParam("response_type", "code")
      .withQueryParam("redirect_uri", redirectUrl)
      .withQueryParam("state", state)
      .withQueryParam("scope", scope.asString)

  def init(cfg: StravaAppCredentials, timeout: FiniteDuration): F[Option[TokenAndScope]] =
    for {
      whenDone <- Deferred[F, Option[TokenAndScope]]
      _ <- Async[F].start(
        Async[F]
          .timeoutTo(whenDone.get.as(true), timeout, whenDone.complete(None))
      )

      state <- Random.scalaUtilRandom
        .flatMap(_.nextBytes(15))
        .map(a => ByteVector.view(a).toBase58)

      uriAndServer <- createServer(cfg, state, whenDone)
      (uri, server) = uriAndServer
      _ <-
        server.use { _ =>
          val authRequestUri = authRequestUrl(cfg, uri, state, StravaScope.activityRead)

          println(s"\nOpen in a browser to complete:\n${authRequestUri.renderString}")
            .flatMap(_ =>
              whenDone.get
                .flatMap {
                  case Some(_) =>
                    println("Authorization successful!")
                  case None =>
                    println("Authorization was not successful!")
                }
            )
        }
      token <- whenDone.get
    } yield token

  def refresh(
      cfg: StravaAppCredentials,
      latestToken: F[TokenAndScope]
  ): F[TokenAndScope] =
    for {
      latestDbToken <- latestToken
      _ <- logger.debug("Refresh latest token")
      refreshed <- tokenRefresh(cfg, latestDbToken.tokenResponse.refreshToken)
    } yield TokenAndScope(refreshed, latestDbToken.scope)

  private def findPort: F[Port] =
    Resource
      .make(Async[F].blocking(new ServerSocket(0)))(ss => Async[F].blocking(ss.close()))
      .use(s => Async[F].blocking(s.getLocalPort).map(Port.fromInt))
      .flatMap {
        case Some(p) => p.pure[F]
        case None    => findPort
      }

  private def createServer(
      cfg: StravaAppCredentials,
      state: String,
      whenDone: Deferred[F, Option[TokenAndScope]]
  ) =
    for {
      port <- findPort
      host <- Host
        .fromString("localhost")
        .map(_.pure[F])
        .getOrElse(Async[F].raiseError(new Exception("invalid host")))

      server = EmberServerBuilder
        .default[F]
        .withHost(host)
        .withPort(port)
        .withShutdownTimeout(1.second)
        .withHttpApp(tokenRoute(cfg, state, whenDone).orNotFound)
        .build
        .onFinalize(logger.debug("Shut down strava token receive service"))

      uri = show"http://$host:$port/keeper/strava"
    } yield (uri, server)

  private def tokenRoute(
      cfg: StravaAppCredentials,
      state: String,
      whenDone: Deferred[F, Option[TokenAndScope]]
  ): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    HttpRoutes.of { case req @ GET -> Root / "keeper" / "strava" =>
      resumeAuthRequest(cfg, state)(req).flatMap {
        case Right(tr) => whenDone.complete(tr.some) >> Ok(tr)
        case Left(err) => Forbidden(err)
      }
    }
  }

  def resumeAuthRequest(
      cfg: StravaAppCredentials,
      state: String
  )(req: Request[F]): F[Either[String, TokenAndScope]] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    val resp: F[Either[String, TokenAndScope]] =
      req.params.get("error") match {
        case Some(err) => s"Strava disallowed access: $err".asLeft[TokenAndScope].pure[F]

        case None =>
          logger.debug(s"Strava responded successful: ${req.params}") *>
            (req.params.get("code"), req.params.get("scope"), req.params.get("state"))
              .mapN { (code, scope, receivedState) =>
                if (state != receivedState)
                  "The request did not deliver the correct state from the process initiation"
                    .asLeft[TokenAndScope]
                    .pure[F]
                else
                  tokenExchange(code, cfg)
                    .map(r => TokenAndScope(r, StravaScope(scope)))
                    .attempt
                    .flatMap {
                      case Right(r) => Right(r).pure[F]
                      case Left(ex) =>
                        logger
                          .error("Error in token exchange", ex)
                          .as(Left(s"Error in token exchange!"))
                    }
              }
              .getOrElse(
                s"Strava did not respond with an access code".asLeft.pure[F]
              )
      }

    resp.flatMap { r =>
      logger.debug("Authorize flow done.").as(r)
    }
  }

  def tokenExchange(code: String, cfg: StravaAppCredentials): F[StravaTokenResponse] = {
    val dsl = Http4sClientDsl[F]
    import dsl._

    val req =
      POST(config.tokenUri)
        .withEntity(
          UrlForm(
            "client_id" -> cfg.clientId,
            "client_secret" -> cfg.clientSecret,
            "code" -> code,
            "grant_type" -> "authorization_code"
          )
        )

    logger.debug(s"Issue token exchange request to ${config.tokenUri}") *>
      client
        .expect[StravaTokenResponse](req)
        .attempt
        .flatTap {
          case Left(ex) => logger.error("Error in token exchange", ex)
          case Right(r) => logger.debug(s"Got token response: $r")
        }
        .rethrow
  }

  def tokenRefresh(cfg: StravaAppCredentials, refreshToken: StravaRefreshToken) = {
    val dsl = Http4sClientDsl[F]
    import dsl._

    val req =
      POST(config.tokenUri)
        .withEntity(
          UrlForm(
            "client_id" -> cfg.clientId,
            "client_secret" -> cfg.clientSecret,
            "refresh_token" -> refreshToken.token,
            "grant_type" -> "refresh_token"
          )
        )

    logger.debug(s"Issue refresh token request to ${config.tokenUri}") *>
      client
        .expect[StravaTokenResponse](req)
        .attempt
        .flatTap {
          case Left(ex) => logger.error("Error in token refresh", ex)
          case Right(r) => logger.debug(s"Got token response: $r")
        }
        .rethrow
  }

  private def println(m: String): F[Unit] =
    Async[F].blocking(Predef.println(m))
}
