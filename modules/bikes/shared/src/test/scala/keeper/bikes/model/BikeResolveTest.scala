package keeper.bikes.model

import java.time.Instant

import cats.Id

import munit.FunSuite

class BikeResolveTest extends FunSuite {

  test("resolve to bike") {
    val testBuild = TestComponentSource.defaultBuild
    val resolver = new BikesResolve[Id](TestComponentSource)
    val bikes = resolver.resolve(testBuild, Instant.EPOCH).toOption.get.bikes
    assertEquals(bikes.size, 2)
    val bike1 = bikes.find(_.id == TestComponentSource.bike1).get
    assertEquals(bike1.frontWheel.map(_.id), Some(TestComponentSource.frontWheel1))
    assertEquals(bike1.fork.map(_.id), Some(TestComponentSource.fork1))
  }
}
