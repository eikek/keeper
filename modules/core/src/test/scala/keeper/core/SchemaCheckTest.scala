package keeper.core

import cats.data.NonEmptyList

import munit.FunSuite

class SchemaCheckTest extends FunSuite with TestSchema {

  val check = new SchemaCheck(types.get)

  test("validate not crashing on missing type") {
    val badCheck = new SchemaCheck(types.removed(rearWheel1).get)
    val result = badCheck.validate(defaultBuild, schema)
    assert(result.isInvalid)
    val errors = result.fold(identity, _ => sys.error("unexpected"))
    assertEquals(errors.size, 1)
    assertEquals(errors.head, SchemaCheck.TypeNotFound(NonEmptyList.of(rearWheel1)))
  }

  test("validate fails") {
    val badBuild =
      DeviceBuild
        .subComponents(rearWheel1)
        .modify(_ + cassette2) // add a second cassette
        .andThen(
          // put a seatpost on a brake-pad
          DeviceBuild.subComponents(brakePad1).modify(_ + seatpost1)
        )
        .andThen(
          // add a second rear wheel
          DeviceBuild.deviceComponents(bike1).modify(_ + rearWheel2)
        )
        .apply(defaultBuild)

    val result = check.validate(badBuild, schema)
    assert(result.isInvalid)
    val errors = result.fold(identity, _ => sys.error("unexpected"))
    assertEquals(errors.size, 4)
    val summary =
      errors.toList.map {
        case _: SchemaCheck.InvalidOccurrence[?] => 1
        case _: SchemaCheck.InvalidPlacement[?]  => 2
        case _: SchemaCheck.DuplicateComponents  => 3
        case _                                   => 0
      }.sorted

    assertEquals(summary, List(1, 1, 2, 3))
  }

  test("validate ok") {
    assert(check.validate(defaultBuild, schema).isValid)
  }
}
