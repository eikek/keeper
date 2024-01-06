package keeper.core

import munit.FunSuite

class DeviceBuildTest extends FunSuite with TestData {

  test("lenses") {
    assertEquals(defaultBuild.findDevice(chain1), Some(bike1))

    val result =
      DeviceBuild.allDeviceComponents.modify(cs => cs - chain1).apply(defaultBuild)
    assertEquals(result.findDevice(chain1), None)
  }

  test("avoid circles") {
    val ev1 = ConfigEvent.SubComponentAdd(None, brakePad1, fork1)
    val ev2 = ConfigEvent.SubComponentAdd(None, frontWheel1, frontWheel1)

    intercept[RuntimeException] {
      defaultBuild.applyAll(Seq(ev1, ev2)).check
    }
  }
}
