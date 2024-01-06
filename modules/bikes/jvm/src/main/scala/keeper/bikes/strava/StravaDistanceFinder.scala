package keeper.bikes.strava

import java.time.{Duration, Instant}

import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.all.*

import keeper.bikes.DistanceFinder
import keeper.bikes.data.Device
import keeper.bikes.model.BikeTotal
import keeper.bikes.util.DateUtil
import keeper.strava.StravaService

final class StravaDistanceFinder[F[_]: Sync](
    client: StravaService[F]
) extends DistanceFinder[F] {
  private[this] val logger = scribe.cats.effect[F]

  def findDistanceAt(date: Instant, bikes: List[Device]): F[Option[List[BikeTotal]]] =
    DateUtil.isCurrent(date, delta = Duration.ofHours(24)).flatMap {
      case true =>
        val bikeMap = bikes.map(d => d.name.toLowerCase -> d.id).toMap

        logger.debug(s"Finding distances for bikes ${bikeMap.keySet} with Strava") >>
          checkToken.semiflatMap { _ =>
            client.getAthlete
              .flatTap(a => logger.debug(s"Got strava athlete: $a"))
              .map(athlete =>
                athlete.bikes
                  .flatMap(g =>
                    bikeMap.get(g.name.toLowerCase).map(id => BikeTotal(id, g.distance))
                  )
              )
          }.value

      case false => Option.empty.pure[F]
    }

  private def checkToken =
    OptionT(client.findToken)
      .flatTapNone(
        logger.info(
          s"Strava configured, but no token available. Please connect to strava"
        )
      )

}
