package keeper.bikes.db.postgres

import java.time.Instant

import cats.data.NonEmptySet
import cats.effect.*
import cats.syntax.all.*

import keeper.bikes.data.{Device, DeviceWithBrand, NewDevice}
import keeper.bikes.db.DeviceRepository
import keeper.core.DeviceId

import skunk.Session

final class PostgresDeviceRepo[F[_]: Sync](session: Resource[F, Session[F]])
    extends DeviceRepository[F] {

  def storeDevice(dev: NewDevice): F[DeviceId] =
    session.use(s =>
      s.transaction
        .use(_ => s.unique(DeviceSql.insert)(dev))
    )

  def findById(id: DeviceId): F[Option[Device]] =
    session.use(_.option(DeviceSql.findById)(id))

  def findByIdWithBrand(id: DeviceId, at: Instant): F[Option[DeviceWithBrand]] =
    session.use(_.option(DeviceSql.findByIdWithBrand)(id -> at))

  def findAll(until: Instant): F[List[DeviceWithBrand]] =
    session.use(_.execute(DeviceSql.findAll)(until))

  def removeDevices(ids: NonEmptySet[DeviceId], date: Instant): F[Unit] =
    session.use { s =>
      s.execute(DeviceSql.updateRemovedAt(ids.toList))(ids -> date)
    }.void
}
