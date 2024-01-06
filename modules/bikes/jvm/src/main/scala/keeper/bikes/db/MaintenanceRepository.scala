package keeper.bikes.db

import java.time.Instant

import fs2.Stream

import keeper.bikes.Page
import keeper.bikes.model.{BikeService, ServiceDetail, ServiceSearchMask}
import keeper.bikes.service.TranslatedEvent
import keeper.core.*

trait MaintenanceRepository[F[_]] {
  def findBikeServices(until: Option[Instant], page: Page): F[List[BikeService]]

  def storeAsMaintenance(bs: BikeService): F[MaintenanceId]

  def storeEvents(id: MaintenanceId, events: List[TranslatedEvent]): F[Unit]

  def maintenanceFromLatestCached: F[(MaintenanceBuild, Stream[F, Maintenance])]

  def maintenanceFromLatestCachedUntil(
      at: Instant
  ): F[(MaintenanceBuild, Stream[F, Maintenance])]

  def generateMissingCache: F[Unit]

  def getServiceDetails(search: ServiceSearchMask): Stream[F, ServiceDetail]
}
