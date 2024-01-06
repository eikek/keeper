package keeper.core

import cats.Show
import cats.data.{NonEmptyList, ValidatedNel}
import cats.syntax.all.*

import keeper.common.Node
import keeper.core.SchemaCheck._

import io.bullet.borer.Encoder

final class SchemaCheck[A: Show](types: ComponentId => Option[A]) {
  private val typeOf: ComponentId => A =
    id => types(id).getOrElse(sys.error(s"No type for $id, slipped through the check"))

  def validate(
      builds: DeviceBuild,
      schema: Schema[A]
  ): Result[DeviceBuild] =
    checkTypes(DeviceBuild.allComponentIds(builds)).andThen { _ =>
      val deviceTrees = builds.devices.keySet.map(devId =>
        DeviceTree(
          devId,
          builds
            .getDeviceTree(devId)
            .map(_.mapValues(cid => ComponentWithType(cid, typeOf(cid))))
            .toList
        )
      )
      val validatedTrees =
        deviceTrees.map(checkDevice(_, schema)).toList.sequence

      (checkDuplicates(builds) |+| validatedTrees.void).as(builds)
    }

  private def checkDuplicates(build: DeviceBuild): Result[Unit] =
    val (_, dupes) =
      build.components.values
        .foldLeft((Set.empty[ComponentId], Set.empty[ComponentId])) {
          case ((acc, ids), b) =>
            val dupes = acc.intersect(b)
            if (dupes.isEmpty) (acc ++ b, ids)
            else (acc ++ b, dupes ++ ids)
        }
    NonEmptyList
      .fromList(dupes.toList)
      .map(nel => SchemaCheck.DuplicateComponents(nel).invalidNel)
      .getOrElse(().validNel)

  private def checkDevice(dev: DeviceTree[A], schema: Schema[A]): Result[DeviceTree[A]] =
    (checkOccurrence(
      dev.deviceId,
      dev.components.map(_.value),
      schema.topLevel
    ) +: dev.components
      .map(c =>
        schema.findTopLevel(c.value.ctype) match
          case Some(schemaNode) =>
            checkComponent(dev.deviceId, c, schemaNode)

          case None =>
            SchemaCheck.InvalidTopLevel(NonEmptyList.of(c.value)).invalidNel
      )).sequence
      .as(dev)

  private def checkComponent(
      devId: DeviceId,
      component: Node[ComponentWithType[A]],
      schema: Node[Schema.Value[A]]
  ): Result[Unit] =
    (checkOccurrence(devId, component.children.map(_.value), schema.children) +:
      component.children
        .map(c =>
          schema.children.find(_.value.ctype == c.value.ctype) match
            case Some(schemaNode) =>
              checkComponent(devId, c, schemaNode)

            case None =>
              SchemaCheck.InvalidPlacement(component.value, c.value, schema).invalidNel
        )).sequence.void

  private def checkOccurrence(
      deviceId: DeviceId,
      components: Seq[ComponentWithType[A]],
      schema: Seq[Node[Schema.Value[A]]]
  ): Result[Unit] =
    val provided =
      components
        .groupBy(_.ctype)
        .map { case (ct, chunk) => ct -> chunk.map(_.id).toSet }
    val known = schema.map(n => n.value.ctype -> n.value.maxOccurrence).toMap
    val errors: List[SchemaError] =
      provided.flatMap { case (ct, ids) =>
        known.get(ct) match
          case Some(max) =>
            if (ids.size > max) Seq(SchemaCheck.InvalidOccurrence(deviceId, ct, ids, max))
            else Seq.empty
          case None =>
            // invalid placement is checked elsewhere
            Seq.empty
      }.toList
    NonEmptyList.fromList(errors) match
      case Some(nel) => nel.invalid
      case None      => ().valid

  private def checkTypes(
      ids: Set[ComponentId]
  ): Result[Unit] =
    val missing =
      ids
        .map(id => id -> types(id))
        .filter(_._2.isEmpty)
        .map(_._1)
    NonEmptyList.fromList(missing.toList) match
      case None      => ().validNel
      case Some(nel) => SchemaCheck.TypeNotFound(nel).invalidNel
}

object SchemaCheck:
  type Result[A] = ValidatedNel[SchemaError, A]

  final case class DeviceTree[A](
      deviceId: DeviceId,
      components: Seq[Node[ComponentWithType[A]]]
  )
  final case class ComponentWithType[A](id: ComponentId, ctype: A)
  object ComponentWithType:
    given componentWithTypeShow[A: Show]: Show[ComponentWithType[A]] =
      Show.show(e => show"${e.ctype}:${e.id}")

  sealed abstract class SchemaError(msg: String) extends RuntimeException(msg)
  object SchemaError:
    given Encoder[SchemaError] =
      Encoder[Map[String, String]].contramap(err => Map("message" -> err.getMessage))

  final case class TypeNotFound(ids: NonEmptyList[ComponentId])
      extends SchemaError(s"The components type could not be determined: $ids")

  final case class InvalidTopLevel[A: Show](errors: NonEmptyList[ComponentWithType[A]])
      extends SchemaError(
        show"These components must not be mounted directly on a bike frame: $errors"
      )

  final case class InvalidPlacement[A: Show](
      parent: ComponentWithType[A],
      component: ComponentWithType[A],
      schema: Node[Schema.Value[A]]
  ) extends SchemaError(
        show"The component $component, mounted on $parent is incorrectly placed for ${schema.value.ctype} at this position."
      )

  final case class InvalidOccurrence[A](
      deviceId: DeviceId,
      ctype: A,
      ids: Set[ComponentId],
      max: Int
  ) extends SchemaError(
        s"Components $ids of type $ctype appeared more than allowed ($max) in device $deviceId."
      )

  final case class DuplicateComponents(ids: NonEmptyList[ComponentId])
      extends SchemaError(s"Components $ids is mounted more than once")
