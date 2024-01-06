package keeper.server

import java.util.UUID

import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.all.*

import keeper.bikes.BikeShop
import keeper.client.data.StravaConnectState
import keeper.server.util.{ClientRequestInfo, MoreHttp4sDsl}
import keeper.strava.StravaService
import keeper.strava.data.StravaScope

import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Location

final class StravaRoutes[F[_]: Sync](
    keeper: BikeShop[F]
) extends Http4sDsl[F]
    with MoreHttp4sDsl[F] {

  private[this] val randomState =
    UUID.randomUUID().toString.take(8)

  def routes: HttpRoutes[F] = keeper.stravaService match
    case Some(svc) => routes(svc)
    case None => HttpRoutes.liftF(OptionT.liftF(Ok(StravaConnectState(false, false))))

  def routes(strava: StravaService[F]): HttpRoutes[F] = HttpRoutes.of {
    case GET -> Root =>
      strava.findToken.flatMap {
        case Some(_) => Ok(StravaConnectState(true, true))
        case None    => Ok(StravaConnectState(true, false))
      }

    case req @ GET -> Root / "connect" =>
      val resume = ClientRequestInfo
        .getBaseUrl(req)
        .map(base => base / "api" / "strava" / "connect" / "resume")
        .getOrElse(req.uri / "resume")

      val url = strava.createAuthUrl(
        resume.renderString,
        randomState,
        StravaScope.activityRead
      )
      TemporaryRedirect(Location(url))

    case req @ GET -> Root / "connect" / "resume" =>
      strava.resumeAuth(randomState, req).flatMap {
        case Right(_) =>
          Ok("Strava connection successful. Return to the app")

        case Left(err) =>
          Forbidden(err)
      }

  }
}
