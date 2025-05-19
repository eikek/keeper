package keeper.webview.client.newbike

import keeper.bikes.data.{ComponentType, ComponentWithDevice}
import keeper.bikes.event.*
import keeper.common.Lenses
import keeper.core.ComponentId

import monocle.{Lens, Monocle, Optional}

final case class ConfigModel(
    data: Map[ComponentType, List[ComponentWithDevice]] = Map.empty,
    showMountedParts: Boolean = false,
    frontWheel: Option[ConfiguredFrontWheel] = None,
    rearWheel: Option[ConfiguredRearWheel] = None,
    handlebar: Option[ComponentId] = None,
    seatpost: Option[ComponentId] = None,
    saddle: Option[ComponentId] = None,
    stem: Option[ComponentId] = None,
    chain: Option[ComponentId] = None,
    crankSet: Option[ComponentId] = None,
    rearBrake: Option[ConfiguredBrake] = None,
    fork: Option[ConfiguredFork] = None,
    frontDerailleur: Option[ComponentId] = None,
    rearDerailleur: Option[ComponentId] = None,
    rearMudguard: Option[ComponentId] = None
):
  def setData(list: List[ComponentWithDevice]): ConfigModel =
    copy(data = list.groupBy(_.component.product.productType))

object ConfigModel:
  val data: Lens[ConfigModel, Map[ComponentType, List[ComponentWithDevice]]] =
    Lens[ConfigModel, Map[ComponentType, List[ComponentWithDevice]]](_.data)(a =>
      _.copy(data = a)
    )

  def dataAt(ct: ComponentType): Lens[ConfigModel, List[ComponentWithDevice]] =
    data
      .andThen(
        Monocle.at[Map[ComponentType, List[ComponentWithDevice]], ComponentType, Option[
          List[ComponentWithDevice]
        ]](ct)
      )
      .andThen(Lenses.optionToEmpty[List[ComponentWithDevice]])

  val showMountedParts: Lens[ConfigModel, Boolean] =
    Lens[ConfigModel, Boolean](_.showMountedParts)(a => _.copy(showMountedParts = a))

  val handlebar: Lens[ConfigModel, Option[ComponentId]] =
    Lens[ConfigModel, Option[ComponentId]](_.handlebar)(a => _.copy(handlebar = a))

  val stem: Lens[ConfigModel, Option[ComponentId]] =
    Lens[ConfigModel, Option[ComponentId]](_.stem)(a => _.copy(stem = a))

  val chain: Lens[ConfigModel, Option[ComponentId]] =
    Lens[ConfigModel, Option[ComponentId]](_.chain)(a => _.copy(chain = a))

  val frontWheel: Lens[ConfigModel, Option[ConfiguredFrontWheel]] =
    Lens[ConfigModel, Option[ConfiguredFrontWheel]](_.frontWheel)(a =>
      _.copy(frontWheel = a)
    )

  val frontWheelId = frontWheel.some.andThen(ConfiguredFrontWheel.id)
  val frontWheelTire: Optional[ConfigModel, Option[ComponentId]] =
    frontWheel.some.andThen(ConfiguredFrontWheel.tire)

  val frontWheelTube: Optional[ConfigModel, Option[ComponentId]] =
    frontWheel.some.andThen(ConfiguredFrontWheel.tube)

  val frontWheelDisc: Optional[ConfigModel, Option[ComponentId]] =
    frontWheel.some.andThen(ConfiguredFrontWheel.brakeDisc)

  val rearWheel: Lens[ConfigModel, Option[ConfiguredRearWheel]] =
    Lens[ConfigModel, Option[ConfiguredRearWheel]](_.rearWheel)(a =>
      _.copy(rearWheel = a)
    )

  val rearWheelId = rearWheel.some.andThen(ConfiguredRearWheel.id)
  val rearWheelDisc = rearWheel.some.andThen(ConfiguredRearWheel.brakeDisc)
  val rearWheelTire = rearWheel.some.andThen(ConfiguredRearWheel.tire)
  val cassette = rearWheel.some.andThen(ConfiguredRearWheel.cassette)
  val rearWheelTube = rearWheel.some.andThen(ConfiguredRearWheel.tube)

  val fork: Lens[ConfigModel, Option[ConfiguredFork]] =
    Lens[ConfigModel, Option[ConfiguredFork]](_.fork)(a => _.copy(fork = a))

  val forkId = fork.some.andThen(ConfiguredFork.id)
  val forkBrake = fork.some.andThen(ConfiguredFork.brake)
  val forkMudguard = fork.some.andThen(ConfiguredFork.mudguard)
  val forkBrakeId = forkBrake.some.andThen(ConfiguredBrake.id)
  val forkBrakePad = forkBrake.some.andThen(ConfiguredBrake.pad)

  val rearBrake: Lens[ConfigModel, Option[ConfiguredBrake]] =
    Lens[ConfigModel, Option[ConfiguredBrake]](_.rearBrake)(a => _.copy(rearBrake = a))

  val rearBrakeId = rearBrake.some.andThen(ConfiguredBrake.id)
  val rearBrakePad = rearBrake.some.andThen(ConfiguredBrake.pad)

  val saddle: Lens[ConfigModel, Option[ComponentId]] =
    Lens[ConfigModel, Option[ComponentId]](_.saddle)(a => _.copy(saddle = a))

  val frontDerailleur: Lens[ConfigModel, Option[ComponentId]] =
    Lens[ConfigModel, Option[ComponentId]](_.frontDerailleur)(a =>
      _.copy(frontDerailleur = a)
    )

  val rearDerailleur: Lens[ConfigModel, Option[ComponentId]] =
    Lens[ConfigModel, Option[ComponentId]](_.rearDerailleur)(a =>
      _.copy(rearDerailleur = a)
    )

  val crankSet: Lens[ConfigModel, Option[ComponentId]] =
    Lens[ConfigModel, Option[ComponentId]](_.crankSet)(a => _.copy(crankSet = a))

  val rearMudguard: Lens[ConfigModel, Option[ComponentId]] =
    Lens[ConfigModel, Option[ComponentId]](_.rearMudguard)(a => _.copy(rearMudguard = a))

  val seatpost: Lens[ConfigModel, Option[ComponentId]] =
    Lens[ConfigModel, Option[ComponentId]](_.seatpost)(a => _.copy(seatpost = a))

  def setter(ct: ComponentType): Option[ComponentId] => ConfigModel => ConfigModel =
    id =>
      ct match
        case ComponentType.Handlebar =>
          handlebar.replace(id)

        case ComponentType.Stem =>
          stem.replace(id)

        case ComponentType.Chain =>
          chain.replace(id)
        case ComponentType.FrontWheel =>
          id.map(i =>
            frontWheel.modify(ow =>
              ow.map(ConfiguredFrontWheel.id.replace(i))
                .orElse(Some(ConfiguredFrontWheel(i)))
            )
          ).getOrElse(frontWheel.replace(None))

        case ComponentType.RearWheel =>
          id.map(i =>
            rearWheel.modify(or =>
              or.map(ConfiguredRearWheel.id.replace(i))
                .orElse(Some(ConfiguredRearWheel(i)))
            )
          ).getOrElse(rearWheel.replace(None))
        case ComponentType.Fork =>
          id.map(i =>
            fork.modify(oc =>
              oc.map(ConfiguredFork.id.replace(i)).orElse(Some(ConfiguredFork(i)))
            )
          ).getOrElse(fork.replace(None))
        case ComponentType.Saddle =>
          saddle.replace(id)
        case ComponentType.FrontDerailleur =>
          frontDerailleur.replace(id)
        case ComponentType.RearDerailleur =>
          rearDerailleur.replace(id)
        case ComponentType.RearMudguard =>
          rearMudguard.replace(id)
        case ComponentType.Seatpost =>
          seatpost.replace(id)
        case ComponentType.CrankSet =>
          crankSet.replace(id)
        case _ =>
          identity
