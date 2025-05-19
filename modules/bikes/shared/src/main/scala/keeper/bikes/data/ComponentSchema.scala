package keeper.bikes.data

import keeper.common.Node
import keeper.core.*
import keeper.core.Schema.Value

object ComponentSchema:
  val get: Schema[ComponentType] =
    Schema.of(
      Node.of(Value(ComponentType.Handlebar, 1)),
      Node.of(Value(ComponentType.Stem, 1)),
      Node.of(Value(ComponentType.Saddle, 1)),
      Node.of(
        Value(ComponentType.Fork, 1),
        Node.of(Value(ComponentType.FrontMudguard, 1)),
        Node.of(
          Value(ComponentType.FrontBrake, 1),
          Node.of(Value(ComponentType.BrakePad, 1))
        )
      ),
      Node.of(Value(ComponentType.RearDerailleur, 1)),
      Node.of(Value(ComponentType.FrontDerailleur, 1)),
      Node.of(Value(ComponentType.Seatpost, 1)),
      Node.of(Value(ComponentType.CrankSet, 1)),
      Node.of(
        Value(ComponentType.RearBrake, 1),
        Node.of(Value(ComponentType.BrakePad, 1))
      ),
      Node.of(Value(ComponentType.RearMudguard, 1)),
      Node.of(Value(ComponentType.Chain, 1)),
      Node.of(
        Value(ComponentType.FrontWheel, 1),
        Node.of(Value(ComponentType.Tire, 1)),
        Node.of(Value(ComponentType.InnerTube, 1)),
        Node.of(Value(ComponentType.BrakeDisc, 1))
      ),
      Node.of(
        Value(ComponentType.RearWheel, 1),
        Node.of(Value(ComponentType.Cassette, 1)),
        Node.of(Value(ComponentType.Tire, 1)),
        Node.of(Value(ComponentType.InnerTube, 1)),
        Node.of(Value(ComponentType.BrakeDisc, 1))
      )
    )

  def check(
      types: ComponentId => Option[ComponentType]
  ): DeviceBuild => SchemaCheck.Result[DeviceBuild] =
    build => new SchemaCheck[ComponentType](types).validate(build, get)
