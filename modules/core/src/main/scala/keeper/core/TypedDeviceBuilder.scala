package keeper.core

import cats.Monad
import cats.data.{NonEmptyList, StateT}
import cats.syntax.all.*

import keeper.core.DeviceBuilder.given

final class TypedDeviceBuilder[F[_]: Monad, A](typeInfo: TypeInfo[F, A]):
  private def findByType(build: DeviceBuild, dev: DeviceId, componentType: A) =
    typeInfo
      .findByType(componentType)
      .map(_.filter(id => build.findDevice(id).contains(dev)))

  private def findSubsByType(build: DeviceBuild, parent: ComponentId, ct: A) =
    typeInfo.findByType(ct).map(_.filter(id => build.findParent(id).contains(parent)))

  def findByType(
      dev: DeviceId,
      componentType: A
  ): StateT[F, DeviceBuild, Set[ComponentId]] =
    StateT.inspectF(build => findByType(build, dev, componentType))

  def findSubByType(
      parent: ComponentId,
      ct: A
  ): StateT[F, DeviceBuild, Set[ComponentId]] =
    StateT.inspectF(build => findSubsByType(build, parent, ct))

  /** Find all sub-components at leaf of `path`. */
  def findSubsByPath(device: DeviceId, path: NonEmptyList[A]) =
    path.tail.foldLeft(findByType(device, path.head))((res, next) =>
      res.flatMap(x => x.map(pid => findSubByType(pid, next)).toSeq.combineAll)
    )

  /** Remove components of the given type. */
  def removeFromDeviceByType(
      dev: DeviceId,
      ct: A
  ): DeviceBuilder[F] =
    for {
      comps <- findByType(dev, ct)
      evs <- comps.toSeq.flatTraverse(cid => DeviceBuilder.removeComponent(dev, cid))
    } yield evs

  /** Removes sub components from the given parent if they are of the given type */
  def removeFromComponentByType(
      parent: ComponentId,
      ct: A
  ): DeviceBuilder[F] =
    for {
      comps <- findSubByType(parent, ct)
      evs <- comps.toSeq.flatTraverse(cid =>
        DeviceBuilder.removeSubComponent(parent, cid)
      )
    } yield evs

  /** Replaces the component on the given bike by removing it from wherever it is
    * currently mounted, removing the same type of component from the given bike and
    * finally adding the component to the bike.
    */
  def replaceOnDevice(
      dev: DeviceId,
      component: ComponentId,
      ct: A
  ): DeviceBuilder[F] =
    DeviceBuilder.combine(
      DeviceBuilder.unmountFromOtherDevice(dev, component),
      removeFromDeviceByType(dev, ct),
      DeviceBuilder.addToDevice(dev, component)
    )

  /** Same as [[replaceOnBike]] but for sub components. */
  def replaceOnComponent(
      component: ComponentId,
      sub: ComponentId,
      ct: A
  ): DeviceBuilder[F] =
    DeviceBuilder.combine(
      DeviceBuilder.unmountFromOtherComponent(component, sub),
      removeFromComponentByType(component, ct),
      DeviceBuilder.addToComponent(component, sub)
    )

  /** Zooms down to last component of `parents` and removes all its sub-components of type
    * `sub`.
    */
  def removeSubComponentByTypeN(
      device: DeviceId,
      subComponents: NonEmptyList[A]
  ): DeviceBuilder[F] =
    if (subComponents.tail.isEmpty) removeFromDeviceByType(device, subComponents.head)
    else
      val init = NonEmptyList.fromListUnsafe(subComponents.init)
      val last = subComponents.last
      for {
        subs <- findSubsByPath(device, init)
        remove <- subs.toSeq.flatTraverse(c => removeFromComponentByType(c, last))
      } yield remove

  /** Zooms into component `parent` and removes all its sub-components of type `sub`. */
  def removeSubComponentByType(
      device: DeviceId,
      parent: A,
      sub: A
  ): DeviceBuilder[F] =
    removeSubComponentByTypeN(device, NonEmptyList.of(parent, sub))

  /** Finds all sub-components at `subComponents.init` and replaces its sub-components
    * with `newSub`. This only makes sense if the path to `sub` denotes a single
    * component.
    */
  def replaceSubComponentByTypeN(
      device: DeviceId,
      subComponents: NonEmptyList[A],
      newSub: ComponentId
  ): DeviceBuilder[F] =
    if (subComponents.tail.isEmpty) replaceOnDevice(device, newSub, subComponents.head)
    else
      val init = NonEmptyList.fromListUnsafe(subComponents.init)
      val last = subComponents.last
      for {
        subs <- findSubsByPath(device, init)
        evs <- subs.toSeq.flatTraverse(c => replaceOnComponent(c, newSub, last))
      } yield evs

  /** Zooms into `parent` component and replaces its sub-components of type `sub` by a
    * single `newSub` sub-component.
    */
  def replaceSubComponentByType(
      device: DeviceId,
      parent: A,
      sub: A,
      newSub: ComponentId
  ): DeviceBuilder[F] =
    for {
      fws <- findByType(device, parent)
      evs <- fws.toSeq.flatTraverse(c => replaceOnComponent(c, newSub, sub))
    } yield evs
