package keeper.bikes.model

import cats.kernel.Eq

import keeper.common.Distance
import keeper.common.borer.BaseCodec.given
import keeper.core.{ComponentId, DeviceId}

import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.{Decoder, Encoder}
import monocle.Lens

final case class BikeBuilds(
    bikes: List[Bike],
    rearWheels: List[RearWheel],
    frontWheels: List[FrontWheel],
    forks: List[Fork],
    bikeTotals: List[BikeTotal],
    componentTotals: Map[ComponentId, Distance]
):
  def isEmpty: Boolean =
    bikes.isEmpty && rearWheels.isEmpty && frontWheels.isEmpty && forks.isEmpty

  def nonEmpty: Boolean = !isEmpty

  def bikeTotal(id: DeviceId): Option[Distance] =
    bikeTotals.find(_.bikeId == id).map(_.distance)
  def compTotal(id: ComponentId): Option[Distance] = componentTotals.get(id)

object BikeBuilds:
  given Encoder[BikeBuilds] = deriveEncoder
  given Decoder[BikeBuilds] = deriveDecoder
  given Eq[BikeBuilds] = Eq.fromUniversalEquals

  val empty: BikeBuilds = BikeBuilds(Nil, Nil, Nil, Nil, Nil, Map.empty)

  val bikes: Lens[BikeBuilds, List[Bike]] =
    Lens[BikeBuilds, List[Bike]](_.bikes)(a => _.copy(bikes = a))

  val rearWheels: Lens[BikeBuilds, List[RearWheel]] =
    Lens[BikeBuilds, List[RearWheel]](_.rearWheels)(a => _.copy(rearWheels = a))

  val frontWheels: Lens[BikeBuilds, List[FrontWheel]] =
    Lens[BikeBuilds, List[FrontWheel]](_.frontWheels)(a => _.copy(frontWheels = a))

  val forks: Lens[BikeBuilds, List[Fork]] =
    Lens[BikeBuilds, List[Fork]](_.forks)(a => _.copy(forks = a))

  val componentTotals: Lens[BikeBuilds, Map[ComponentId, Distance]] =
    Lens[BikeBuilds, Map[ComponentId, Distance]](_.componentTotals)(a =>
      _.copy(componentTotals = a)
    )

  val bikeTotals: Lens[BikeBuilds, List[BikeTotal]] =
    Lens[BikeBuilds, List[BikeTotal]](_.bikeTotals)(a => _.copy(bikeTotals = a))
