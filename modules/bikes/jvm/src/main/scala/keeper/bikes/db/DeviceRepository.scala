package keeper.bikes.db

import java.time.Instant

import cats.data.NonEmptySet

import keeper.bikes.data.{Device, DeviceWithBrand, NewDevice}
import keeper.core.DeviceId

trait DeviceRepository[F[_]] {

  def storeDevice(dev: NewDevice): F[DeviceId]

  def findById(id: DeviceId): F[Option[Device]]

  def findByIdWithBrand(id: DeviceId, at: Instant): F[Option[DeviceWithBrand]]

  def findAll(until: Instant): F[List[DeviceWithBrand]]

  def removeDevices(ids: NonEmptySet[DeviceId], date: Instant): F[Unit]
}
