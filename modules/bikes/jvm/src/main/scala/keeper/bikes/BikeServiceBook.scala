package keeper.bikes

import java.time.{Instant, ZoneId}

import fs2.Stream

import keeper.bikes.model.*

trait BikeServiceBook[F[_]] {

  def generateMissingCacheEntries: F[Unit]

  def getServiceDetail(mask: ServiceSearchMask): Stream[F, ServiceDetail]

  def getBikesAt(
      date: Option[Instant],
      zoneId: ZoneId
  ): F[Either[BikesResolveError, BikeBuilds]]

  def getDistances(at: Option[Instant], zoneId: ZoneId): F[List[BikeTotal]]
}
