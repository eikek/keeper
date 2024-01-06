package keeper.core

import cats.syntax.all.*

import keeper.common.{Lenses, Node}

import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.{Decoder, Encoder}
import monocle.function.all.*
import monocle.{Lens, Traversal}

final case class DeviceBuild(
    devices: Map[DeviceId, Set[ComponentId]],
    components: Map[ComponentId, Set[ComponentId]]
):
  def check: DeviceBuild =
    DeviceBuild.checkCircles(this)
    this

  def applyEvent(ev: ConfigEvent): DeviceBuild =
    DeviceBuild.eventChange(ev)(this)

  def applyAll(events: Seq[ConfigEvent]): DeviceBuild =
    events.foldLeft(this)(_ applyEvent _)

  def findDevice(componentId: ComponentId): Option[DeviceId] = {
    @annotation.tailrec
    def loop(id: ComponentId): Option[DeviceId] =
      devices.find(_._2.contains(id)) match
        case Some((dev, _)) => Some(dev)
        case None =>
          findParent(id) match
            case Some(parent) => loop(parent)
            case None         => None

    loop(componentId)
  }

  def findParent(componentId: ComponentId): Option[ComponentId] =
    components.find(_._2.contains(componentId)).map(_._1)

  def getDeviceTree(deviceId: DeviceId): Set[Node[ComponentId]] =
    def makeNode(cid: ComponentId): Node[ComponentId] =
      Node(cid, components.getOrElse(cid, Set.empty).toSeq.map(makeNode))
    devices.get(deviceId) match
      case None     => Set.empty
      case Some(tl) => tl.map(makeNode)

  def deviceComponentsRecursive(deviceId: DeviceId) =
    getDeviceTree(deviceId).flatMap(_.traverse.map(_.value).compile.toList.toSet)

  def subComponentsRecursive(id: ComponentId): Set[ComponentId] =
    components.getOrElse(id, Set.empty).flatMap(sid => subComponentsRecursive(sid) + sid)

object DeviceBuild:
  private case class DeviceBuildArr(
      devices: List[(DeviceId, Set[ComponentId])],
      components: List[(ComponentId, Set[ComponentId])]
  )

  given Encoder[DeviceBuild] = deriveEncoder[DeviceBuildArr].contramap(b =>
    DeviceBuildArr(b.devices.toList, b.components.toList)
  )
  given Decoder[DeviceBuild] = deriveDecoder[DeviceBuildArr].map(b =>
    DeviceBuild(b.devices.toMap, b.components.toMap)
  )

  private def checkCircles(build: DeviceBuild) =
    hasCircles(build).fold(
      errs => sys.error(s"The component tree contains circles: $errs"),
      _ => ()
    )

  def hasCircles(
      build: DeviceBuild
  ): Either[Map[ComponentId, Set[ComponentId]], DeviceBuild] =
    val circles = findCircles(build).components
    if (circles.isEmpty) Right(build)
    else Left(circles)

  @annotation.tailrec
  private def findCircles(build: DeviceBuild): DeviceBuild = {
    val parents = build.components.keySet
    val children = build.components.values.toSet.flatten.toSeq
    val result =
      children.foldLeft(build) { (dev, cid) =>
        dev
          .findParent(cid)
          .filter(_ => !parents.contains(cid))
          .map(pid => DeviceBuild.subComponents(pid).modify(_ - cid).apply(dev))
          .getOrElse(dev)
      }
    if (result == build) result
    else findCircles(result)
  }

  val empty: DeviceBuild = DeviceBuild(Map.empty, Map.empty)

  val devices: Lens[DeviceBuild, Map[DeviceId, Set[ComponentId]]] =
    Lens[DeviceBuild, Map[DeviceId, Set[ComponentId]]](_.devices)(a =>
      _.copy(devices = a)
    )

  val components: Lens[DeviceBuild, Map[ComponentId, Set[ComponentId]]] =
    Lens[DeviceBuild, Map[ComponentId, Set[ComponentId]]](_.components)(a =>
      _.copy(components = a)
    )

  def deviceComponentsOpt(devId: DeviceId): Lens[DeviceBuild, Option[Set[ComponentId]]] =
    devices
      .andThen(
        at[Map[DeviceId, Set[ComponentId]], DeviceId, Option[Set[ComponentId]]](devId)
      )

  def deviceComponents(devId: DeviceId): Lens[DeviceBuild, Set[ComponentId]] =
    deviceComponentsOpt(devId)
      .andThen(Lenses.optionToEmpty[Set[ComponentId]])

  val allDeviceComponents = devices.andThen(Lenses.mapValues[DeviceId, Set[ComponentId]])

  def subComponents(id: ComponentId): Lens[DeviceBuild, Set[ComponentId]] =
    components
      .andThen(
        at[Map[ComponentId, Set[ComponentId]], ComponentId, Option[Set[ComponentId]]](id)
      )
      .andThen(Lenses.optionToEmpty[Set[ComponentId]])

  val allSubComponents: Traversal[DeviceBuild, Set[ComponentId]] =
    components.andThen(Lenses.mapValues[ComponentId, Set[ComponentId]])

  val allComponentIds: DeviceBuild => Set[ComponentId] =
    (allDeviceComponents.getAll, allSubComponents.getAll).mapN((a, b) =>
      a.toSet.flatten ++ b.toSet.flatten
    )

  def eventChange(ev: ConfigEvent): DeviceBuild => DeviceBuild =
    ev match
      case ConfigEvent.ComponentDrop(dev, id) =>
        components.modify(_.removed(id))

      case ConfigEvent.DeviceDrop(id) =>
        devices.modify(_.removed(id))

      case ConfigEvent.ComponentAdd(dev, comp) =>
        allDeviceComponents
          .modify(_ - comp)
          .andThen(deviceComponents(dev).modify(_ + comp))

      case ConfigEvent.ComponentRemove(dev, comp) =>
        deviceComponents(dev).modify(_ - comp)

      case ConfigEvent.SubComponentAdd(_, comp, sub) =>
        allSubComponents
          .modify(_ - sub)
          .andThen(subComponents(comp).modify(_ + sub))

      case ConfigEvent.SubComponentRemove(_, comp, sub) =>
        subComponents(comp).modify(_ - sub)
