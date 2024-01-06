package keeper.bikes.model

import java.time.Instant

import cats.Eq

import keeper.bikes.data.ActionName
import keeper.bikes.event.ServiceEventName
import keeper.common.borer.BaseCodec.given
import keeper.core.{ComponentId, DeviceId}

import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.{Decoder, Encoder}
import monocle.Lens

final case class ServiceSearchMask(
    affectedBikes: Set[DeviceId] = Set.empty,
    affectedComponents: Set[ComponentId] = Set.empty,
    actions: Set[ActionName] = Set.empty,
    serviceEventNames: Set[ServiceEventName] = Set.empty,
    untilDate: Option[Instant] = None
)

object ServiceSearchMask:
  given Eq[ServiceSearchMask] = Eq.fromUniversalEquals
  given Encoder[ServiceSearchMask] = deriveEncoder
  given Decoder[ServiceSearchMask] = deriveDecoder

  val affectedBikes: Lens[ServiceSearchMask, Set[DeviceId]] =
    Lens[ServiceSearchMask, Set[DeviceId]](_.affectedBikes)(a =>
      _.copy(affectedBikes = a)
    )
  val affectedComponents: Lens[ServiceSearchMask, Set[ComponentId]] =
    Lens[ServiceSearchMask, Set[ComponentId]](_.affectedComponents)(a =>
      _.copy(affectedComponents = a)
    )
  val actions: Lens[ServiceSearchMask, Set[ActionName]] =
    Lens[ServiceSearchMask, Set[ActionName]](_.actions)(a => _.copy(actions = a))
  val serviceEventNames: Lens[ServiceSearchMask, Set[ServiceEventName]] =
    Lens[ServiceSearchMask, Set[ServiceEventName]](_.serviceEventNames)(a =>
      _.copy(serviceEventNames = a)
    )
  val untilDate: Lens[ServiceSearchMask, Option[Instant]] =
    Lens[ServiceSearchMask, Option[Instant]](_.untilDate)(a => _.copy(untilDate = a))
