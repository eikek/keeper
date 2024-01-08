package keeper.bikes.service

import java.time.{Instant, ZoneId}

import cats.effect.{Clock, Sync}
import cats.syntax.all.*

import keeper.bikes.db.{Inventory, MaintenanceRepository}
import keeper.bikes.model.*
import keeper.bikes.{BikeServiceBook, DistanceFinder}

final class DefaultBikeServiceBook[F[_]: Sync](
    inventory: Inventory[F],
    repo: MaintenanceRepository[F],
    distanceFinder: DistanceFinder[F]
) extends BikeServiceBook[F] {
  def generateMissingCacheEntries: F[Unit] =
    repo.generateMissingCache

  def getServiceDetail(mask: ServiceSearchMask): fs2.Stream[F, ServiceDetail] =
    repo.getServiceDetails(mask)

  def getBikesAt(
      date: Option[Instant],
      zoneId: ZoneId
  ): F[Either[BikesResolveError, BikeBuilds]] =
    getDistances(date, zoneId).flatMap { dst =>
      date
        .map(inventory.getBikesAt(_, dst))
        .getOrElse(inventory.getCurrentBikes(dst))
    }

  def getDistances(at: Option[Instant], zoneId: ZoneId): F[List[BikeTotal]] =
    for {
      now <- Clock[F].realTimeInstant
      ts = at.getOrElse(now)
      devs <- inventory.devices.findAll(ts)
      kms <- distanceFinder.findDistanceAt(ts, devs.map(_.device))
    } yield kms.getOrElse(Nil)
}
