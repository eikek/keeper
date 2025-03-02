package keeper.bikes.data

import java.time.Instant

import cats.syntax.all.*

import keeper.common.borer.syntax.all.given
import keeper.core.*

import io.bullet.borer.NullOptions.given
import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.{Decoder, Encoder}

final case class MaintenanceEvent(
    id: MaintenanceEventId,
    maintenance: MaintenanceId,
    index: Int,
    action: ActionName,
    device: Option[DeviceId],
    component: Option[ComponentId],
    subComponent: Option[ComponentId],
    createdAt: Instant
):

  def toConfigEvent: Option[ConfigEvent] =
    (component, subComponent) match
      case (Some(cid), Some(sid)) =>
        action
          .fold0(
            ConfigEvent.SubComponentAdd(device, cid, sid).some,
            ConfigEvent.SubComponentRemove(device, cid, sid).some,
            None
          )
          .flatten

      case _ =>
        (device, component) match
          case ((Some(dev), Some(cid))) =>
            action
              .fold0(
                ConfigEvent.ComponentAdd(dev, cid),
                ConfigEvent.ComponentRemove(dev, cid),
                ConfigEvent.ComponentDrop(dev.some, cid)
              )

          case (dev, Some(cid)) =>
            action
              .fold0(None, None, ConfigEvent.ComponentDrop(dev, cid).some)
              .flatten

          case (Some(dev), None) =>
            action.fold0(None, None, ConfigEvent.DeviceDrop(dev).some).flatten

          case _ => None

object MaintenanceEvent:
  given Encoder[MaintenanceEvent] = deriveEncoder
  given Decoder[MaintenanceEvent] = deriveDecoder
