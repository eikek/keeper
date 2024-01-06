package keeper.core

trait TestData {

  val bike1 = DeviceId(1)
  val bike2 = DeviceId(2)

  val frontWheel1 = ComponentId(1)
  val frontWheel2 = ComponentId(2)
  val rearWheel1 = ComponentId(3)
  val rearWheel2 = ComponentId(4)

  val chain1 = ComponentId(5)
  val chain2 = ComponentId(6)

  val cassette1 = ComponentId(7)
  val cassette2 = ComponentId(8)

  val tire1 = ComponentId(9)
  val tire2 = ComponentId(10)
  val tire3 = ComponentId(11)
  val tire4 = ComponentId(12)

  val fork1 = ComponentId(13)
  val fork2 = ComponentId(14)

  val frontBrake1 = ComponentId(15)
  val frontBrake2 = ComponentId(16)

  val brakePad1 = ComponentId(17)
  val brakePad2 = ComponentId(18)
  val brakePad3 = ComponentId(19)
  val brakePad4 = ComponentId(20)

  val brakeDisc1 = ComponentId(21)
  val brakeDisc2 = ComponentId(22)
  val brakeDisc3 = ComponentId(23)
  val brakeDisc4 = ComponentId(24)

  val chain3 = ComponentId(25)
  val chain4 = ComponentId(26)
  val rearBrake1 = ComponentId(27)
  val rearBrake2 = ComponentId(28)

  val seatpost1 = ComponentId(29)
  val seatpost2 = ComponentId(30)

  val defaultBuild = DeviceBuild(
    devices = Map(
      bike1 -> Set(frontWheel1, rearWheel1, fork1, chain1, rearBrake1, seatpost1),
      bike2 -> Set(frontWheel2, rearWheel2, fork2, chain2, rearBrake2, seatpost2)
    ),
    components = Map(
      frontWheel1 -> Set(brakeDisc1, tire1),
      rearWheel1 -> Set(brakeDisc2, tire2, cassette1),
      frontWheel2 -> Set(brakeDisc3, tire3),
      rearWheel2 -> Set(brakeDisc4, tire4, cassette2),
      fork1 -> Set(frontBrake1),
      fork2 -> Set(frontBrake2),
      frontBrake1 -> Set(brakePad1),
      frontBrake2 -> Set(brakePad2),
      rearBrake1 -> Set(brakePad3),
      rearBrake2 -> Set(brakePad4)
    )
  )

}
