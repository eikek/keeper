package keeper.core

import keeper.common.Node
import keeper.core.Schema.Value

trait TestSchema extends TestData {

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
    frontBrake2 -> ComponentType.FrontBrake,
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
    rearBrake1 -> ComponentType.RearBrake,
    rearBrake2 -> ComponentType.RearBrake,
    seatpost1 -> ComponentType.Seatpost,
    seatpost2 -> ComponentType.Seatpost
  )

  val schema: Schema[ComponentType] =
    Schema.of(
      Node.of(
        Value(ComponentType.Fork, 1),
        Node.of(
          Value(ComponentType.FrontBrake, 1),
          Node.of(Value(ComponentType.BrakePad, 1))
        )
      ),
      Node.of(Value(ComponentType.Seatpost, 1)),
      Node.of(
        Value(ComponentType.RearBrake, 1),
        Node.of(Value(ComponentType.BrakePad, 1))
      ),
      Node.of(Value(ComponentType.Chain, 1)),
      Node.of(
        Value(ComponentType.FrontWheel, 1),
        Node.of(Value(ComponentType.Tire, 1)),
        Node.of(Value(ComponentType.BrakeDisc, 1))
      ),
      Node.of(
        Value(ComponentType.RearWheel, 1),
        Node.of(Value(ComponentType.Cassette, 1)),
        Node.of(Value(ComponentType.Tire, 1)),
        Node.of(Value(ComponentType.BrakeDisc, 1))
      )
    )
}
