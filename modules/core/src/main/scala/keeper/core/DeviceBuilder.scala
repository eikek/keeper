package keeper.core

import cats.data.StateT
import cats.syntax.all.*
import cats.{Applicative, Monad, Monoid}

/** Tracks the configuration changes to a [[DeviceBuild]] emitting the new state. It
  * provides a high-level view of changing a device, resulting in 0-n low-level
  * [[ConfigEvent]]s
  */
type DeviceBuilder[F[_]] = StateT[F, DeviceBuild, Seq[ConfigEvent]]

object DeviceBuilder:
  given _stateTMonoid[F[_]: Monad, A, B: Monoid]: Monoid[StateT[F, A, B]] =
    Monoid.instance(
      StateT.empty,
      (a, b) => a.flatMap(ra => b.map(rb => Monoid[B].combine(ra, rb)))
    )

  def doNothing[F[_]: Applicative]: DeviceBuilder[F] =
    StateT.empty

  def combineAll[F[_]: Monad](bs: Seq[DeviceBuilder[F]]): DeviceBuilder[F] =
    bs.sequence.map(_.flatten)

  def combine[F[_]: Monad](bs: DeviceBuilder[F]*): DeviceBuilder[F] =
    combineAll(bs)

  def findDevice[F[_]: Applicative](
      componentId: ComponentId
  ): StateT[F, DeviceBuild, Option[DeviceId]] =
    StateT.inspect(_.findDevice(componentId))

  def findParent[F[_]: Applicative](
      component: ComponentId
  ): StateT[F, DeviceBuild, Option[ComponentId]] =
    StateT.inspect(_.findParent(component))

  def applyEvents[F[_]: Applicative](
      events: ConfigEvent*
  ): DeviceBuilder[F] =
    if (events.isEmpty) doNothing[F]
    else StateT(builds => (builds.applyAll(events), events).pure[F])

  def addToDevice[F[_]: Applicative](
      bike: DeviceId,
      component: ComponentId
  ): DeviceBuilder[F] =
    applyEvents(ConfigEvent.ComponentAdd(bike, component))

  def addToComponent[F[_]: Monad](
      parent: ComponentId,
      sub: ComponentId
  ): DeviceBuilder[F] =
    for {
      dev <- findDevice(parent)
      ev <- applyEvents(ConfigEvent.SubComponentAdd(dev, parent, sub))
    } yield ev

  def dropDevice[F[_]: Monad](
      deviceId: DeviceId,
      withComponents: Boolean
  ): DeviceBuilder[F] = {
    val remDev =
      StateT.inspect(DeviceBuild.devices.get).map(_.contains(deviceId)).flatMap {
        case true  => applyEvents(ConfigEvent.DeviceDrop(deviceId))
        case false => doNothing[F]
      }

    val removeComps = StateT
      .inspect(DeviceBuild.deviceComponents(deviceId).get)
      .flatMap(cids => combineAll(cids.toSeq.map(dropComponent(_, withComponents))))

    (if (withComponents) removeComps else doNothing[F]) |+| remDev
  }

  def dropComponent[F[_]: Monad](
      id: ComponentId,
      withSubs: Boolean
  ): DeviceBuilder[F] = {
    val subs = StateT
      .inspect(DeviceBuild.subComponents(id).get)
      .flatMap(sids => combineAll(sids.toSeq.map(dropComponent(_, withSubs))))

    val dropComp =
      StateT
        .inspect(DeviceBuild.components.get)
        .map(_.contains(id))
        .flatMap {
          case true =>
            findDevice(id).flatMap(dev => applyEvents(ConfigEvent.ComponentDrop(dev, id)))
          case false => doNothing[F]
        }

    (if (withSubs) subs else doNothing[F]) |+| dropComp
  }

  def removeComponent[F[_]: Applicative](
      bike: DeviceId,
      component: ComponentId
  ): DeviceBuilder[F] =
    applyEvents(ConfigEvent.ComponentRemove(bike, component))

  def removeSubComponent[F[_]: Monad](
      parent: ComponentId,
      sub: ComponentId
  ): DeviceBuilder[F] =
    for {
      dev <- findDevice(parent)
      ev <- applyEvents(ConfigEvent.SubComponentRemove(dev, parent, sub))
    } yield ev

  def removeFromAll[F[_]: Monad](id: ComponentId): DeviceBuilder[F] =
    for {
      dev <- findDevice(id)
      parent <- findParent(id)
      remDev = dev.fold(doNothing[F])(devId => removeComponent(devId, id))
      remSub <- parent.fold(remDev)(pId => removeSubComponent(pId, id))
    } yield remSub

  /** remove from other device where it is currently mounted */
  def unmountFromOtherDevice[F[_]: Monad](
      bike: DeviceId,
      component: ComponentId
  ): DeviceBuilder[F] =
    for {
      dev <- findDevice(component)
      next <- dev match
        case Some(d) if d != bike => removeComponent(d, component)
        case _                    => doNothing
    } yield next

  /** Remove the sub-component from its parent if it is on a different one that specified.
    */
  def unmountFromOtherComponent[F[_]: Monad](
      component: ComponentId,
      sub: ComponentId
  ): DeviceBuilder[F] =
    for {
      parent <- findParent(sub)
      next <- parent match
        case Some(p) if p != component => removeSubComponent(p, sub)
        case _                         => doNothing
    } yield next

  /** Remove the component if it is on the given bike. */
  def unmountFromDevice[F[_]: Monad](
      bike: DeviceId,
      component: ComponentId
  ): DeviceBuilder[F] =
    for {
      dev <- findDevice(component)
      next <- dev match
        case Some(d) if d == bike => removeComponent(d, component)
        case _                    => doNothing
    } yield next

  def unmountFromComponent[F[_]: Monad](
      component: ComponentId,
      sub: ComponentId
  ): DeviceBuilder[F] =
    for {
      parent <- findParent(sub)
      next <- parent match
        case Some(p) if p == component => removeSubComponent(p, component)
        case _                         => doNothing
    } yield next

  def withTypes[F[_]: Monad, A](typeInfo: TypeInfo[F, A]): TypedDeviceBuilder[F, A] =
    new TypedDeviceBuilder[F, A](typeInfo)
