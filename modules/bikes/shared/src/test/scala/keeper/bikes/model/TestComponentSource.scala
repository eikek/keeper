package keeper.bikes.model

import java.time.Instant

import cats.Id
import cats.data.NonEmptyList

import keeper.bikes.data.*
import keeper.core.{ComponentType as _, *}

object TestComponentSource extends ComponentSource[Id] with TestData {
  val brand = Brand(BrandId(1), "Campa", None, Instant.EPOCH)

  val types = Map(
    frontWheel1 -> ComponentType.FrontWheel,
    frontWheel2 -> ComponentType.FrontWheel,
    rearWheel1 -> ComponentType.RearWheel,
    rearWheel2 -> ComponentType.RearWheel,
    chain1 -> ComponentType.Chain,
    chain2 -> ComponentType.Chain,
    cassette1 -> ComponentType.Cassette,
    cassette2 -> ComponentType.Cassette,
    tire1 -> ComponentType.Tire,
    tire2 -> ComponentType.Tire,
    tire3 -> ComponentType.Tire,
    tire4 -> ComponentType.Tire,
    fork1 -> ComponentType.Fork,
    fork2 -> ComponentType.Fork,
    frontBrake1 -> ComponentType.FrontBrake,
    rearBrake1 -> ComponentType.RearBrake,
    brakePad1 -> ComponentType.BrakePad,
    brakePad2 -> ComponentType.BrakePad,
    brakePad3 -> ComponentType.BrakePad,
    brakePad4 -> ComponentType.BrakePad,
    brakeDisc1 -> ComponentType.BrakeDisc,
    brakeDisc2 -> ComponentType.BrakeDisc,
    brakeDisc3 -> ComponentType.BrakeDisc,
    brakeDisc4 -> ComponentType.BrakeDisc,
    chain3 -> ComponentType.Chain,
    chain4 -> ComponentType.Chain,
    frontBrake2 -> ComponentType.FrontBrake,
    rearBrake2 -> ComponentType.RearBrake,
    seatpost1 -> ComponentType.Seatpost,
    seatpost2 -> ComponentType.Seatpost
  )

  def getComponentsOfType(
      at: Instant,
      cts: NonEmptyList[ComponentType]
  ): Id[Seq[ComponentWithProduct]] =
    types.toList.collect {
      case (id, ct) if cts.toList.contains(ct) => makeComponentWithProduct(id, ct)
    }

  override def findDevice(id: DeviceId, at: Instant): Id[Option[DeviceWithBrand]] =
    Some(
      DeviceWithBrand(
        Device(
          id,
          brand.id,
          s"device $id",
          None,
          ComponentState.Active,
          Instant.EPOCH,
          None,
          Instant.EPOCH
        ),
        brand
      )
    )

  override def findComponent(
      id: ComponentId,
      at: Instant
  ): Id[Option[ComponentWithProduct]] =
    types.get(id).map { ct =>
      makeComponentWithProduct(id, ct)
    }

  private def makeComponentWithProduct(id: ComponentId, ct: ComponentType) =
    val pid = ProductId(id.asLong + 1)
    ComponentWithProduct(
      BikeProduct(
        pid,
        brand.id,
        ct,
        s"product $ct",
        None,
        None,
        Instant.EPOCH
      ),
      brand,
      Component(
        id = id,
        product = pid,
        name = s"Component $id",
        description = None,
        state = ComponentState.Active,
        addedAt = Instant.EPOCH,
        None,
        initialTotal = TotalOutput.zero,
        createdAt = Instant.EPOCH
      )
    )
}
