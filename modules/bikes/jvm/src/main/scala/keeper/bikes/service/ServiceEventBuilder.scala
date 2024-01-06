package keeper.bikes.service

import cats.data.StateT
import cats.syntax.all.*
import cats.{Applicative, Functor, Monad}

import keeper.bikes.data.{ActionName, NewMaintenanceEvent}
import keeper.core._

type ServiceEventBuilder[F[_]] = StateT[F, DeviceBuild, Seq[NewMaintenanceEvent]]

object ServiceEventBuilder:
  private val unknownMaintenance: MaintenanceId = MaintenanceId(-1)
  private val unknownIndex: Int = -1

  def noop[F[_]: Applicative]: ServiceEventBuilder[F] =
    StateT.empty

  def inspect[F[_]: Applicative, A](f: DeviceBuild => A): StateT[F, DeviceBuild, A] =
    StateT.inspect(f)

  def fromDeviceBuilder[F[_]: Functor](db: DeviceBuilder[F]): ServiceEventBuilder[F] =
    db.map(
      _.map(NewMaintenanceEvent.fromConfigEvent(unknownMaintenance, unknownIndex))
    )

  def findDevice[F[_]: Applicative](
      componentId: ComponentId
  ): StateT[F, DeviceBuild, Option[DeviceId]] =
    inspect(_.findDevice(componentId))

  def deviceExists[F[_]: Applicative](
      deviceId: DeviceId
  ): StateT[F, DeviceBuild, Boolean] =
    inspect(_.devices.contains(deviceId))

  def subComponentsRecursive[F[_]: Applicative](
      cid: ComponentId
  ): StateT[F, DeviceBuild, Set[ComponentId]] =
    inspect(_.subComponentsRecursive(cid))

  def deviceComponentsRecursive[F[_]: Applicative](
      deviceId: DeviceId
  ): StateT[F, DeviceBuild, Set[ComponentId]] =
    inspect(_.deviceComponentsRecursive(deviceId))

  def componentActions[F[_]: Monad](
      action: ActionName,
      ids: Seq[ComponentId]
  ): ServiceEventBuilder[F] =
    ids.flatTraverse(componentAction(action, _))

  def componentAction[F[_]: Applicative](
      action: ActionName,
      componentId: ComponentId
  ): ServiceEventBuilder[F] =
    for {
      dev <- findDevice[F](componentId)
    } yield Seq(mkEvent(action, dev, componentId.some, None))

  def bikeAction[F[_]: Applicative](
      action: ActionName,
      bikes: Seq[DeviceId]
  ): ServiceEventBuilder[F] =
    inspect(_.devices.keySet.intersect(bikes.toSet)).map(ids =>
      ids.map(id => mkEvent(action, id.some, None, None)).toSeq
    )

  def ceaseComponent[F[_]: Monad](id: ComponentId, withSubs: Boolean) =
    for {
      dev <- findDevice(id)
      subs <-
        if (withSubs) subComponentsRecursive(id)
        else StateT.pure[F, DeviceBuild, Set[ComponentId]](Set.empty)

      ev0 = mkEvent(ActionName.Cease, dev, id.some, None)
      evs = subs.map(sid => mkEvent(ActionName.Cease, dev, id.some, sid.some))
    } yield ev0 +: evs.toSeq

  def ceaseBike[F[_]: Monad](id: DeviceId, withComps: Boolean) =
    for {
      comps <-
        if (withComps) deviceComponentsRecursive(id)
        else StateT.pure[F, DeviceBuild, Set[ComponentId]](Set.empty)

      ev0 = mkEvent(ActionName.Cease, id.some, None, None)
      evs = comps.map(cid => mkEvent(ActionName.Cease, id.some, cid.some, None))
    } yield ev0 +: evs.toSeq

  private def mkEvent(
      action: ActionName,
      device: Option[DeviceId],
      component: Option[ComponentId],
      subComponent: Option[ComponentId]
  ) =
    NewMaintenanceEvent(
      unknownMaintenance,
      unknownIndex,
      action,
      device,
      component,
      subComponent
    )

  extension [F[_]: Functor](self: DeviceBuilder[F])
    def asServiceEventBuilder: ServiceEventBuilder[F] =
      fromDeviceBuilder(self)

  extension [F[_]: Functor](self: ServiceEventBuilder[F])
    def setMaintenance(id: MaintenanceId): ServiceEventBuilder[F] =
      self.map(_.map(NewMaintenanceEvent.maintenance.replace(id)))

    def setIndex(offset: Int): StateT[F, DeviceBuild, (Int, Seq[NewMaintenanceEvent])] =
      self.map(in =>
        (
          in.size + offset,
          in.zipWithIndex.map { case (ev, idx) =>
            NewMaintenanceEvent.index.replace(offset + idx)(ev)
          }
        )
      )
