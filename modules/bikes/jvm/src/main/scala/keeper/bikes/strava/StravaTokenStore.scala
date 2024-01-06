package keeper.bikes.strava

import cats.effect.{Resource, Sync}
import cats.syntax.all.*

import keeper.bikes.db.postgres.Codecs as c
import keeper.strava.TokenStore
import keeper.strava.data.TokenAndScope

import skunk.Session
import skunk.codec.all as a
import skunk.implicits.*

final class StravaTokenStore[F[_]: Sync](session: Resource[F, Session[F]])
    extends TokenStore[F] {

  private val insert =
    sql"""
       insert into "strava_token"
         (client_id, token_type, access_token, refresh_token, expires_at, expires_in, scope)
       values
         (${a.varchar}, ${a.varchar}, ${c.stravaAccessToken}, ${c.stravaRefreshToken}, ${c.instant}, ${c.javaDuration}, ${c.stravaScopes})
       """.command
      .contramap[(String, TokenAndScope)] { case (cid, TokenAndScope(token, scope)) =>
        cid *: token.tokenType *: token.accessToken *: token.refreshToken *: token.expiresAt *: token.expiresIn *: scope *: EmptyTuple
      }

  private val find =
    sql"""
       select token_type, access_token, expires_at, expires_in, refresh_token, scope
       from "strava_token"
       where client_id = ${a.varchar}
       order by created_at desc
       limit 1
       """.query(c.stravaTokenAndScope)

  def findLatest(clientId: String): F[Option[TokenAndScope]] =
    session.use(_.option(find)(clientId))

  def store(clientId: String, token: TokenAndScope): F[Unit] =
    session.use(s => s.transaction.use(_ => s.execute(insert)(clientId -> token)).void)
}
