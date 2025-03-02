package keeper.bikes.model

import java.time.Instant

import cats.{Eq, Order}

import keeper.bikes.data.{ActionName, ComponentType}
import keeper.bikes.event.ServiceEventName
import keeper.common.Distance
import keeper.common.borer.BaseCodec.given
import keeper.core.TotalsTracker.EntryMap.given
import keeper.core._

import io.bullet.borer.NullOptions.given
import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.derivation.key
import io.bullet.borer.{Decoder, Encoder}

final case class ServiceDetail(
    id: MaintenanceId,
    name: String,
    description: Option[String],
    date: Instant,
    serviceNames: Set[ServiceEventName],
    affectedBikes: Set[ServiceDetail.AffectedBike],
    entries: List[ServiceDetail.ServiceEntry],
    totals: TotalsTracker.EntryMap
):
  def totalsFor(cid: ComponentId): Option[Distance] =
    totals.get(cid).map(_.running).map(n => Distance.meter(n.asDouble))

object ServiceDetail:
  given Encoder[ServiceDetail] = deriveEncoder
  given Decoder[ServiceDetail] = deriveDecoder
  given Eq[ServiceDetail] = Eq.fromUniversalEquals

  final case class AffectedBike(
      id: DeviceId,
      name: String,
      total: Option[Distance]
  ):
    def toBikeAndName: BikeAndName = BikeAndName(id, name)

  object AffectedBike:
    given Eq[AffectedBike] = Eq.fromUniversalEquals
    given Encoder[AffectedBike] = deriveEncoder
    given Decoder[AffectedBike] = deriveDecoder

  final case class BikeAndName(
      id: DeviceId,
      name: String
  )
  object BikeAndName:
    given Eq[BikeAndName] = Eq.fromUniversalEquals
    given Encoder[BikeAndName] = deriveEncoder
    given Decoder[BikeAndName] = deriveDecoder

  final case class ComponentInfo(
      id: ComponentId,
      name: String,
      @key("type") typ: ComponentType,
      initial: Distance
  )
  object ComponentInfo:
    given Eq[ComponentInfo] = Eq.fromUniversalEquals
    given Encoder[ComponentInfo] = deriveEncoder
    given Decoder[ComponentInfo] = deriveDecoder
    given Order[ComponentInfo] = Order.by(_.id)

  final case class ServiceEntry(
      index: Int,
      action: ActionName,
      serviceName: ServiceEventName,
      bike: Option[BikeAndName] = None,
      component: Option[ComponentInfo] = None,
      sub: Option[ComponentInfo] = None
  )
  object ServiceEntry:
    given Eq[ServiceEntry] = Eq.fromUniversalEquals
    given Encoder[ServiceEntry] = deriveEncoder
    given Decoder[ServiceEntry] = deriveDecoder
