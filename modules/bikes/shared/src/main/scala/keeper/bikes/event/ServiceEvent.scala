package keeper.bikes.event

import java.time.Instant

import cats.data.NonEmptySet
import cats.kernel.Eq

import keeper.bikes.data.*
import keeper.common.borer.BaseCodec.given
import keeper.common.borer.syntax.all.*
import keeper.core.{ComponentId, DeviceId}

import io.bullet.borer.NullOptions.given
import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.{Decoder, Encoder}
import monocle.Lens

sealed trait ServiceEvent extends Product:
  def componentTypes: Seq[(ComponentId, ComponentType)]
  def eventName: ServiceEventName

  def fold[A](
      fb: ServiceEvent.BuildEvent => A,
      fn: ServiceEvent.NonBuildEvent => A,
      fu: ServiceEvent.UnspecificEvent => A
  ): A

object ServiceEvent:
  given Encoder[ServiceEvent] = deriveEncoder
  given Decoder[ServiceEvent] = deriveDecoder
  given Eq[ServiceEvent] = Eq.fromUniversalEquals

  /** Events that change structure of bikes or components, e.g. change tires */
  sealed trait BuildEvent extends ServiceEvent {
    def fold[A](
        fb: ServiceEvent.BuildEvent => A,
        fn: ServiceEvent.NonBuildEvent => A,
        fu: ServiceEvent.UnspecificEvent => A
    ): A = fb(this)
  }

  /** Events that don't change structure of bikes or components, e.g. cleaning */
  sealed trait NonBuildEvent extends ServiceEvent {
    def fold[A](
        fb: ServiceEvent.BuildEvent => A,
        fn: ServiceEvent.NonBuildEvent => A,
        fu: ServiceEvent.UnspecificEvent => A
    ): A = fn(this)
  }

  /** Events that could be of both types */
  sealed trait UnspecificEvent extends NonBuildEvent with BuildEvent {
    override def fold[A](
        fb: ServiceEvent.BuildEvent => A,
        fn: ServiceEvent.NonBuildEvent => A,
        fu: ServiceEvent.UnspecificEvent => A
    ): A = fu(this)
  }

  final case class NewBikeEvent(
      brandId: BrandId,
      name: String,
      description: Option[String],
      addedAt: Instant,
      id: DeviceId = DeviceId(-1),
      frontWheel: Option[ConfiguredFrontWheel] = None,
      rearWheel: Option[ConfiguredRearWheel] = None,
      handlebar: Option[ComponentId] = None,
      seatpost: Option[ComponentId] = None,
      saddle: Option[ComponentId] = None,
      stem: Option[ComponentId] = None,
      chain: Option[ComponentId] = None,
      rearBrake: Option[ConfiguredBrake] = None,
      fork: Option[ConfiguredFork] = None,
      frontDerailleur: Option[ComponentId] = None,
      rearDerailleur: Option[ComponentId] = None,
      rearMudguard: Option[ComponentId] = None,
      crankSet: Option[ComponentId] = None
  ) extends BuildEvent {
    val newDevice: NewDevice =
      NewDevice(brandId, name, description, ComponentState.Active, addedAt)

    val eventName: ServiceEventName = ServiceEventName.NewBike

    val componentTypes: Seq[(ComponentId, ComponentType)] =
      ComponentType.values.toSeq.flatMap {
        case ct @ ComponentType.Chain      => chain.map(_ -> ct).toSeq
        case ct @ ComponentType.FrontWheel => frontWheel.map(c => c.id -> ct).toSeq
        case ct @ ComponentType.RearWheel  => rearWheel.map(c => c.id -> ct).toSeq
        case ct @ ComponentType.Handlebar  => handlebar.map(_ -> ct).toSeq
        case ct @ ComponentType.Seatpost   => seatpost.map(_ -> ct).toSeq
        case ct @ ComponentType.Stem       => stem.map(_ -> ct).toSeq
        case ct @ ComponentType.RearBrake  => rearBrake.map(_.id -> ct)
        case ct @ ComponentType.FrontBrake => fork.flatMap(_.brake).map(_.id -> ct)
        case ct @ ComponentType.Fork       => fork.map(c => c.id -> ct).toSeq
        case ct @ ComponentType.CrankSet   => crankSet.map(_ -> ct).toSeq
        case ct @ ComponentType.FrontDerailleur =>
          frontDerailleur.map(id => id -> ct).toSeq
        case ct @ ComponentType.RearDerailleur => rearDerailleur.map(_ -> ct).toSeq
        case ct @ ComponentType.FrontMudguard =>
          fork.flatMap(_.mudguard).map(_ -> ct).toSeq
        case ct @ ComponentType.RearMudguard => rearMudguard.map(_ -> ct).toSeq
        case ct @ ComponentType.Saddle       => saddle.map(_ -> ct).toSeq
        case ct @ ComponentType.Cassette =>
          rearWheel.flatMap(_.cassette).map(_ -> ct).toSeq
        case ct @ ComponentType.Tire =>
          (rearWheel.flatMap(_.tire).toSeq ++ frontWheel
            .flatMap(_.tire)
            .toSeq)
            .map(_ -> ct)
        case ct @ ComponentType.BrakeDisc =>
          (frontWheel.flatMap(_.brakeDisc).toSeq ++ rearWheel.flatMap(_.brakeDisc).toSeq)
            .map(_ -> ct)
        case ct @ ComponentType.BrakePad =>
          (fork.flatMap(_.brake).flatMap(_.pad).toSeq ++ rearBrake.flatMap(_.pad).toSeq)
            .map(_ -> ct)
        case ct @ ComponentType.InnerTube =>
          (rearWheel.flatMap(_.tube).toSeq ++ frontWheel
            .flatMap(_.tube)
            .toSeq)
            .map(_ -> ct)
      }
  }

  object NewBikeEvent:
    given Encoder[NewBikeEvent] = deriveEncoder
    given Decoder[NewBikeEvent] = deriveDecoder

  final case class ChangeBike(
      bike: DeviceId,
      frontWheel: Alter[ComponentId] = Alter.Discard,
      rearWheel: Alter[ComponentId] = Alter.Discard,
      handlebar: Alter[ComponentId] = Alter.Discard,
      seatpost: Alter[ComponentId] = Alter.Discard,
      saddle: Alter[ComponentId] = Alter.Discard,
      stem: Alter[ComponentId] = Alter.Discard,
      chain: Alter[ComponentId] = Alter.Discard,
      rearBrake: Alter[ComponentId] = Alter.Discard,
      fork: Alter[ComponentId] = Alter.Discard,
      crankSet: Alter[ComponentId] = Alter.Discard,
      frontDerailleur: Alter[ComponentId] = Alter.Discard,
      rearDerailleur: Alter[ComponentId] = Alter.Discard,
      rearMudguard: Alter[ComponentId] = Alter.Discard
  ) extends BuildEvent:
    val eventName: ServiceEventName = ServiceEventName.ChangeBike

    def forType(ct: ComponentType): Alter[ComponentId] =
      ct match
        case ComponentType.FrontWheel      => frontWheel
        case ComponentType.RearWheel       => rearWheel
        case ComponentType.Handlebar       => handlebar
        case ComponentType.Seatpost        => seatpost
        case ComponentType.Saddle          => saddle
        case ComponentType.Stem            => stem
        case ComponentType.Chain           => chain
        case ComponentType.RearBrake       => rearBrake
        case ComponentType.Fork            => fork
        case ComponentType.CrankSet        => crankSet
        case ComponentType.FrontDerailleur => frontDerailleur
        case ComponentType.RearDerailleur  => rearDerailleur
        case ComponentType.RearMudguard    => rearMudguard
        case ComponentType.Tire            => Alter.Discard
        case ComponentType.InnerTube       => Alter.Discard
        case ComponentType.FrontMudguard   => Alter.Discard
        case ComponentType.BrakeDisc       => Alter.Discard
        case ComponentType.Cassette        => Alter.Discard
        case ComponentType.BrakePad        => Alter.Discard
        case ComponentType.FrontBrake      => Alter.Discard

    val isModified: Boolean =
      ComponentType.values.exists(ct => !forType(ct).isDiscard)

    val nonModified: Boolean = !isModified

    val componentTypes: Seq[(ComponentId, ComponentType)] =
      ComponentType.values.toSeq.flatMap(ct => forType(ct).asOption.map(_ -> ct).toSeq)

  object ChangeBike:
    given Encoder[ChangeBike] = deriveEncoder
    given Decoder[ChangeBike] = deriveDecoder

  final case class ChangeFrontWheel(
      wheel: ComponentId,
      tire: Alter[ComponentId] = Alter.Discard,
      disc: Alter[ComponentId] = Alter.Discard,
      tube: Alter[ComponentId] = Alter.Discard
  ) extends BuildEvent:
    val eventName: ServiceEventName = ServiceEventName.ChangeFrontWheel
    def forType(ct: ComponentType): Alter[ComponentId] = ct match
      case ComponentType.Tire      => tire
      case ComponentType.BrakeDisc => disc
      case ComponentType.InnerTube => tube
      case _                       => Alter.Discard

    val componentTypes: Seq[(ComponentId, ComponentType)] =
      ComponentType.values.toSeq.flatMap(ct => forType(ct).asOption.map(_ -> ct).toSeq)

    val isModified: Boolean =
      ComponentType.values.exists(ct => !forType(ct).isDiscard)

    val nonModified: Boolean = !isModified

  object ChangeFrontWheel:
    given Encoder[ChangeFrontWheel] = deriveEncoder
    given Decoder[ChangeFrontWheel] = deriveDecoder

  final case class ChangeRearWheel(
      wheel: ComponentId,
      tire: Alter[ComponentId] = Alter.Discard,
      disc: Alter[ComponentId] = Alter.Discard,
      tube: Alter[ComponentId] = Alter.Discard,
      cassette: Alter[ComponentId] = Alter.Discard
  ) extends BuildEvent:
    val eventName: ServiceEventName = ServiceEventName.ChangeRearWheel

    def forType(ct: ComponentType): Alter[ComponentId] = ct match
      case ComponentType.Tire      => tire
      case ComponentType.InnerTube => tube
      case ComponentType.BrakeDisc => disc
      case ComponentType.Cassette  => cassette
      case _                       => Alter.Discard

    val componentTypes: Seq[(ComponentId, ComponentType)] =
      ComponentType.values.toSeq.flatMap(ct => forType(ct).asOption.map(_ -> ct).toSeq)

    val isModified: Boolean =
      ComponentType.values.exists(ct => !forType(ct).isDiscard)

    val nonModified: Boolean = !isModified

  object ChangeRearWheel:
    given Encoder[ChangeRearWheel] = deriveEncoder
    given Decoder[ChangeRearWheel] = deriveDecoder

  final case class ChangeFork(
      fork: ComponentId,
      mudguard: Alter[ComponentId] = Alter.Discard,
      brake: Alter[ComponentId] = Alter.Discard
  ) extends BuildEvent:
    val eventName: ServiceEventName = ServiceEventName.ChangeFork
    val componentTypes: Seq[(ComponentId, ComponentType)] =
      ComponentType.values.toSeq.flatMap(ct => forType(ct).asOption.map(_ -> ct).toSeq)

    val isModified: Boolean =
      ComponentType.values.exists(ct => !forType(ct).isDiscard)

    val nonModified: Boolean = !isModified

    def forType(ct: ComponentType): Alter[ComponentId] = ct match
      case ComponentType.FrontMudguard => mudguard
      case ComponentType.FrontBrake    => brake
      case _                           => Alter.Discard

  object ChangeFork:
    given Encoder[ChangeFork] = deriveEncoder
    given Decoder[ChangeFork] = deriveDecoder

  final case class ChangeTires(
      bike: DeviceId,
      front: Alter[ComponentId],
      rear: Alter[ComponentId]
  ) extends BuildEvent:
    val eventName: ServiceEventName = ServiceEventName.ChangeTires
    val componentTypes: Seq[(ComponentId, ComponentType)] =
      Seq(front, rear).flatMap(_.asOption).map(_ -> ComponentType.Tire)

  object ChangeTires:
    given Decoder[ChangeTires] = deriveDecoder
    given Encoder[ChangeTires] = deriveEncoder

  final case class ChangeBrakePads(
      bike: DeviceId,
      front: Alter[ComponentId],
      rear: Alter[ComponentId]
  ) extends BuildEvent:
    val eventName: ServiceEventName = ServiceEventName.ChangeBrakePads
    val componentTypes: Seq[(ComponentId, ComponentType)] =
      Seq(front, rear).flatMap(_.asOption).map(_ -> ComponentType.BrakePad)

  object ChangeBrakePads:
    given Decoder[ChangeBrakePads] = deriveDecoder
    given Encoder[ChangeBrakePads] = deriveEncoder

  final case class WaxChain(
      chains: NonEmptySet[ComponentId],
      waxType: WaxChain.WaxType
  ) extends NonBuildEvent:
    val eventName: ServiceEventName = ServiceEventName.WaxChain
    val componentTypes: Seq[(ComponentId, ComponentType)] =
      chains.toSortedSet.toSeq.map(_ -> ComponentType.Chain)

  object WaxChain:
    given Encoder[WaxChain] = deriveEncoder
    given Decoder[WaxChain] = deriveDecoder

    val chains: Lens[WaxChain, NonEmptySet[ComponentId]] =
      Lens[WaxChain, NonEmptySet[ComponentId]](_.chains)(a => _.copy(chains = a))

    val waxType: Lens[WaxChain, WaxType] =
      Lens[WaxChain, WaxType](_.waxType)(a => _.copy(waxType = a))

    enum WaxType:
      case Hot
      case Drip
      def name: String = productPrefix

    object WaxType:
      given Eq[WaxType] = Eq.fromUniversalEquals
      def fromString(str: String): Either[String, WaxType] =
        List(Hot, Drip)
          .find(_.name.equalsIgnoreCase(str))
          .toRight(s"Invalid wax type: str")

      given Encoder[WaxType] = Encoder.forString.contramap(_.productPrefix.toLowerCase)
      given Decoder[WaxType] = Decoder.forString.emap(fromString)

  final case class CeaseComponent(components: NonEmptySet[ComponentId], withSubs: Boolean)
      extends UnspecificEvent:
    val eventName: ServiceEventName = ServiceEventName.CeaseComponent
    val componentTypes: Seq[(ComponentId, ComponentType)] = Seq.empty

  object CeaseComponent:
    given Encoder[CeaseComponent] = deriveEncoder
    given Decoder[CeaseComponent] = deriveDecoder

  final case class CeaseBike(id: DeviceId, withComponents: Boolean)
      extends UnspecificEvent:
    val eventName: ServiceEventName = ServiceEventName.CeaseBike
    val componentTypes: Seq[(ComponentId, ComponentType)] = Seq.empty

  object CeaseBike:
    given Encoder[CeaseBike] = deriveEncoder
    given Decoder[CeaseBike] = deriveDecoder

  final case class PatchTube(
      tubes: NonEmptySet[ComponentId]
  ) extends NonBuildEvent:
    val componentTypes: Seq[(ComponentId, ComponentType)] =
      tubes.toSortedSet.toSeq.map(_ -> ComponentType.InnerTube)
    val eventName: ServiceEventName = ServiceEventName.PatchTube

  object PatchTube:
    given Encoder[PatchTube] = deriveEncoder
    given Decoder[PatchTube] = deriveDecoder

  final case class PatchTire(
      tubes: NonEmptySet[ComponentId]
  ) extends NonBuildEvent:
    val componentTypes: Seq[(ComponentId, ComponentType)] =
      tubes.toSortedSet.toSeq.map(_ -> ComponentType.Tire)
    val eventName: ServiceEventName = ServiceEventName.PatchTire

  object PatchTire:
    given Encoder[PatchTire] = deriveEncoder
    given Decoder[PatchTire] = deriveDecoder

  final case class CleanComponent(
      components: NonEmptySet[ComponentId]
  ) extends NonBuildEvent:
    val componentTypes: Seq[(ComponentId, ComponentType)] = Seq.empty
    override def eventName: ServiceEventName = ServiceEventName.CleanComponent

  object CleanComponent:
    given Encoder[CleanComponent] = deriveEncoder
    given Decoder[CleanComponent] = deriveDecoder

  final case class CleanBike(
      bikes: NonEmptySet[DeviceId]
  ) extends NonBuildEvent:
    val componentTypes: Seq[(ComponentId, ComponentType)] = Seq.empty
    val eventName: ServiceEventName = ServiceEventName.CleanBike

  object CleanBike:
    given Encoder[CleanBike] = deriveEncoder
    given Decoder[CleanBike] = deriveDecoder
