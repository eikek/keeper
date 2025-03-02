package keeper.strava.impl

import java.time.Instant

import scala.concurrent.duration.FiniteDuration

import cats.effect.*
import cats.syntax.all.*
import fs2.io.file.Files
import fs2.io.file.Path
import fs2.io.net.Network

import keeper.http.borer.BorerEntityCodec.Implicits.*
import keeper.strava.data.*
import keeper.strava.{StravaAppCredentials, StravaClient, StravaClientConfig}

import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.{Method, Request, Uri}

final class DefaultStravaClient[F[_]: Async: Network: Files](
    client: Client[F],
    config: StravaClientConfig
) extends StravaClient[F]
    with Http4sClientDsl[F] {

  private val oauth = new StravaOAuth[F](config, client)
  private val upload = new StravaUpload[F](config, client)

  def authRequestUrl(
      cfg: StravaAppCredentials,
      redirectUrl: String,
      state: String,
      scope: StravaScope
  ): Uri =
    oauth.authRequestUrl(cfg, redirectUrl, state, scope)

  def resumeAuthRequest(
      cfg: StravaAppCredentials,
      state: String
  )(req: Request[F]): F[Either[String, TokenAndScope]] =
    oauth.resumeAuthRequest(cfg, state)(req)

  def initAuth(cfg: StravaAppCredentials, timeout: FiniteDuration) =
    oauth.init(cfg, timeout)

  def refreshAuth(
      cfg: StravaAppCredentials,
      latestToken: F[TokenAndScope]
  ) = oauth.refresh(cfg, latestToken)

  def getAthlete(accessToken: StravaAccessToken): F[StravaAthlete] =
    client.expect[StravaAthlete](
      Method.GET(config.apiUrl / "athlete").putAuth(accessToken)
    )

  def listActivities(
      accessToken: StravaAccessToken,
      after: Instant,
      before: Instant,
      page: Int,
      perPage: Int
  ): F[List[StravaActivity]] = {
    val uri = (config.apiUrl / "athlete" / "activities")
      .withQueryParam("before", before.getEpochSecond)
      .withQueryParam("after", after.getEpochSecond)
      .withQueryParam("page", page)
      .withQueryParam("per_page", perPage)

    client.expect[List[StravaActivity]](
      Method.GET(uri).putAuth(accessToken)
    )
  }

  def findGear(
      accessToken: StravaAccessToken,
      gearId: String
  ): F[Option[StravaGear]] = {
    val uri = config.apiUrl / "gear" / gearId
    client.expectOption[StravaGear](
      Method.GET(uri).putAuth(accessToken)
    )
  }

  def updateActivity(
      accessToken: StravaAccessToken,
      id: StravaActivityId,
      data: StravaUpdatableActivity
  ) = {
    val uri = config.apiUrl / "activities" / id
    client
      .successful(
        Method.PUT(data, uri).putAuth(accessToken)
      )
      .flatMap {
        case true => ().pure[F]
        case false =>
          Async[F].raiseError(
            new Exception(s"Activity $id update returned with a failure")
          )
      }
  }

  def uploadFile(
      accessToken: StravaAccessToken,
      externalId: Option[String],
      file: Path,
      fileType: StravaFileType,
      name: String,
      description: Option[String],
      commute: Boolean,
      processingTimeout: FiniteDuration,
      waitingCallback: (FiniteDuration, Int) => F[Unit]
  ): F[Either[StravaUploadError, StravaActivityId]] =
    upload.uploadFile(
      accessToken,
      externalId,
      file,
      fileType,
      name,
      description,
      commute,
      processingTimeout,
      waitingCallback
    )
}
