package keeper.strava.data

import cats.data.NonEmptyList

sealed trait StravaSportType extends Product {
  final def name: String =
    productPrefix
}

object StravaSportType {

  case object AlpineSki extends StravaSportType {}
  case object BackcountrySki extends StravaSportType {}
  case object Badminton extends StravaSportType
  case object Canoeing extends StravaSportType
  case object Crossfit extends StravaSportType
  case object EBikeRide extends StravaSportType
  case object Elliptical extends StravaSportType
  case object EMountainBikeRide extends StravaSportType
  case object Golf extends StravaSportType {}
  case object GravelRide extends StravaSportType {}
  case object Handcycle extends StravaSportType
  case object HighIntensityIntervalTraining extends StravaSportType {}
  case object Hike extends StravaSportType {}
  case object IceSkate extends StravaSportType {}
  case object InlineSkate extends StravaSportType {}
  case object Kayaking extends StravaSportType {}
  case object Kitesurf extends StravaSportType {}
  case object MountainBikeRide extends StravaSportType {}
  case object NordicSki extends StravaSportType {}
  case object Pickleball extends StravaSportType
  case object Pilates extends StravaSportType
  case object Racquetball extends StravaSportType
  case object Ride extends StravaSportType {}
  case object RockClimbing extends StravaSportType {}
  case object RollerSki extends StravaSportType
  case object Rowing extends StravaSportType {}
  case object Run extends StravaSportType {}
  case object Sail extends StravaSportType {}
  case object Skateboard extends StravaSportType
  case object Snowboard extends StravaSportType {}
  case object Snowshoe extends StravaSportType {}
  case object Soccer extends StravaSportType {}
  case object Squash extends StravaSportType
  case object StairStepper extends StravaSportType
  case object StandUpPaddling extends StravaSportType {}
  case object Surfing extends StravaSportType {}
  case object Swim extends StravaSportType {}
  case object TableTennis extends StravaSportType
  case object Tennis extends StravaSportType {}
  case object TrailRun extends StravaSportType {}
  case object Velomobile extends StravaSportType
  case object VirtualRide extends StravaSportType {}
  case object VirtualRow extends StravaSportType {}
  case object VirtualRun extends StravaSportType {}
  case object Walk extends StravaSportType {}
  case object WeightTraining extends StravaSportType {}
  case object Wheelchair extends StravaSportType
  case object Windsurf extends StravaSportType {}
  case object Workout extends StravaSportType {}
  case object Yoga extends StravaSportType

  val all: NonEmptyList[StravaSportType] =
    NonEmptyList.of(
      AlpineSki,
      BackcountrySki,
      Badminton,
      Canoeing,
      Crossfit,
      EBikeRide,
      Elliptical,
      EMountainBikeRide,
      Golf,
      GravelRide,
      Handcycle,
      HighIntensityIntervalTraining,
      Hike,
      IceSkate,
      InlineSkate,
      Kayaking,
      Kitesurf,
      MountainBikeRide,
      NordicSki,
      Pickleball,
      Pilates,
      Racquetball,
      Ride,
      RockClimbing,
      RollerSki,
      Rowing,
      Run,
      Sail,
      Skateboard,
      Snowboard,
      Snowshoe,
      Soccer,
      Squash,
      StairStepper,
      StandUpPaddling,
      Surfing,
      Swim,
      TableTennis,
      Tennis,
      TrailRun,
      Velomobile,
      VirtualRide,
      VirtualRow,
      VirtualRun,
      Walk,
      WeightTraining,
      Wheelchair,
      Windsurf,
      Workout,
      Yoga
    )

  def fromString(str: String): Either[String, StravaSportType] =
    all.find(_.name.equalsIgnoreCase(str)).toRight(s"Invalid strava sport: $str")
}
