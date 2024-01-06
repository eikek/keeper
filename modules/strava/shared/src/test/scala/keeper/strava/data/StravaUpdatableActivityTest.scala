package keeper.strava.data

import io.bullet.borer.*
import munit.*

class StravaUpdatableActivityTest extends FunSuite {

  test("encode") {
    val data = StravaUpdatableActivity(
      commute = Some(true),
      trainer = None,
      description = Some("my description"),
      name = Some("Morning Ride"),
      gearId = Some("123")
    )
    val jsonStr = Json.encode(data).toUtf8String
    assertEquals(
      jsonStr,
      """{"commute":true,"description":"my description","name":"Morning Ride","gear_id":"123"}"""
    )
  }
}
