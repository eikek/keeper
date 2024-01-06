package keeper.bikes.service

import java.time.Instant

import cats.effect.Sync
import cats.syntax.all.*
import fs2.Pipe

import keeper.bikes.data.{Brand, DeviceWithBrand}
import keeper.bikes.db.DeviceRepository
import keeper.bikes.event.ServiceEvent
import keeper.core.{DeviceBuild, DeviceId}

trait NewBikeResolve[F[_]: Sync] {

  def resolveNewBikesEphemeral(
      current: DeviceBuild,
      events: List[ServiceEvent]
  ): List[(ServiceEvent, Seq[DeviceWithBrand])] =
    val max = current.devices.keySet.map(_.asLong).maxOption.getOrElse(1L)
    events.zipWithIndex.map { case (ev, idx) =>
      ev match
        case nb: ServiceEvent.NewBikeEvent =>
          val dev = nb.newDevice.toDevice(DeviceId(max + idx + 1), Instant.EPOCH)
          val someBrand = Brand(nb.brandId, "Some Brand", None, Instant.EPOCH)
          (nb.copy(id = dev.id), Seq(DeviceWithBrand(dev, someBrand)))
        case _ =>
          (ev, Seq.empty)
    }

  def resolveNewBikes(repo: DeviceRepository[F]): Pipe[F, ServiceEvent, ServiceEvent] =
    _.evalMap {
      case ev: ServiceEvent.NewBikeEvent =>
        repo.storeDevice(ev.newDevice).map(id => ev.copy(id = id))

      case ev => ev.pure[F]
    }
}
