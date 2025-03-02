package keeper.bikes.model

import java.time.Instant

import cats.Eq

import keeper.bikes.data.*
import keeper.common.borer.BaseCodec.given
import keeper.core.*

import io.bullet.borer.NullOptions.given
import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.{Decoder, Encoder}
import monocle.Lens

final case class Bike(
    id: DeviceId,
    brand: Brand,
    name: String,
    description: Option[String],
    state: ComponentState,
    addedAt: Instant,
    createdAt: Instant,
    frontWheel: Option[FrontWheel] = None,
    rearWheel: Option[RearWheel] = None,
    handlebar: Option[BasicComponent] = None,
    seatpost: Option[BasicComponent] = None,
    saddle: Option[BasicComponent] = None,
    stem: Option[BasicComponent] = None,
    chain: Option[BasicComponent] = None,
    rearBrake: Option[BrakeCaliper] = None,
    fork: Option[Fork] = None,
    frontDerailleur: Option[BasicComponent] = None,
    rearDerailleur: Option[BasicComponent] = None,
    rearMudguard: Option[BasicComponent] = None
)

object Bike:
  given Encoder[Bike] = deriveEncoder
  given Decoder[Bike] = deriveDecoder
  given Eq[Bike] = Eq.fromUniversalEquals

  val frontWheel: Lens[Bike, Option[FrontWheel]] =
    Lens[Bike, Option[FrontWheel]](_.frontWheel)(a => _.copy(frontWheel = a))

  val rearWheel: Lens[Bike, Option[RearWheel]] =
    Lens[Bike, Option[RearWheel]](_.rearWheel)(a => _.copy(rearWheel = a))

  val handlebar: Lens[Bike, Option[BasicComponent]] =
    Lens[Bike, Option[BasicComponent]](_.handlebar)(a => _.copy(handlebar = a))

  val seatpost: Lens[Bike, Option[BasicComponent]] =
    Lens[Bike, Option[BasicComponent]](_.seatpost)(a => _.copy(seatpost = a))

  val saddle: Lens[Bike, Option[BasicComponent]] =
    Lens[Bike, Option[BasicComponent]](_.saddle)(a => _.copy(saddle = a))

  val stem: Lens[Bike, Option[BasicComponent]] =
    Lens[Bike, Option[BasicComponent]](_.stem)(a => _.copy(stem = a))

  val chain: Lens[Bike, Option[BasicComponent]] =
    Lens[Bike, Option[BasicComponent]](_.chain)(a => _.copy(chain = a))

  val rearBrake: Lens[Bike, Option[BrakeCaliper]] =
    Lens[Bike, Option[BrakeCaliper]](_.rearBrake)(a => _.copy(rearBrake = a))

  val fork: Lens[Bike, Option[Fork]] =
    Lens[Bike, Option[Fork]](_.fork)(a => _.copy(fork = a))

  val frontDerailleur: Lens[Bike, Option[BasicComponent]] =
    Lens[Bike, Option[BasicComponent]](_.frontDerailleur)(a =>
      _.copy(frontDerailleur = a)
    )

  val rearDerailleur: Lens[Bike, Option[BasicComponent]] =
    Lens[Bike, Option[BasicComponent]](_.rearDerailleur)(a => _.copy(rearDerailleur = a))

  val rearMudguard: Lens[Bike, Option[BasicComponent]] =
    Lens[Bike, Option[BasicComponent]](_.rearMudguard)(a => _.copy(rearMudguard = a))
