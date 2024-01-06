package keeper.strava

import keeper.strava.data.TokenAndScope

trait TokenStore[F[_]] {

  def findLatest(clientId: String): F[Option[TokenAndScope]]

  def store(clientId: String, token: TokenAndScope): F[Unit]
}
