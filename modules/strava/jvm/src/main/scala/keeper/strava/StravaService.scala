package keeper.strava

import java.time.Instant

import fs2.Stream

import keeper.strava.data._

import org.http4s.{Request, Uri}

trait StravaService[F[_]] {
  def createAuthUrl(
      redirectUrl: String,
      state: String,
      scope: StravaScope
  ): Uri

  def resumeAuth(
      state: String,
      req: Request[F]
  ): F[Either[String, TokenAndScope]]

  def findToken: F[Option[TokenAndScope]]

  def listActivities(
      after: Instant,
      before: Instant,
      page: Int,
      perPage: Int
  ): F[List[StravaActivity]]

  def listAllActivities(
      after: Instant,
      before: Instant,
      chunkSize: Int
  ): Stream[F, StravaActivity]

  def findGear(gearId: String): F[Option[StravaGear]]

  def getAthlete: F[StravaAthlete]
}
