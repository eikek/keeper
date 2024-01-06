package keeper.strava.data

import io.bullet.borer.*
import munit.*

class StravaAthleteTest extends FunSuite {

  test("decode") {
    val data = getClass
      .getResourceAsStream("/athlete-payload.json")
      .readAllBytes()

    val result = Json.decode(data).to[StravaAthlete].value
    assertEquals(result.id, StravaAthleteId(101))
    assertEquals(result.username, None)
    assertEquals(result.bikes.size, 2)
    assertEquals(result.shoes.size, 1)
  }
}
