package keeper.bikes.data

import keeper.common.Distance

import munit.FunSuite

class DistanceTest extends FunSuite {

  test("fromString") {
    assertEquals(Distance.fromString("1m"), Right(Distance.meter(1)))
    assertEquals(Distance.fromString("1"), Right(Distance.meter(1)))
    assertEquals(Distance.fromString("1km"), Right(Distance.km(1)))
    assertEquals(Distance.fromString("11.45km"), Right(Distance.km(11.45)))
  }
}
