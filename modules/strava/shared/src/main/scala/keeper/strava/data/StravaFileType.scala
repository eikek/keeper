package keeper.strava.data

import cats.data.NonEmptyList

sealed trait StravaFileType extends Product {
  final def name: String =
    productPrefix.toLowerCase match {
      case n if n.endsWith("gz") => s"${n.dropRight(2)}.gz"
      case n                     => n
    }
}

object StravaFileType {
  case object Fit extends StravaFileType
  case object FitGz extends StravaFileType
  case object Tcx extends StravaFileType
  case object TcxGz extends StravaFileType
  case object Gpx extends StravaFileType
  case object GpxGz extends StravaFileType

  val all: NonEmptyList[StravaFileType] =
    NonEmptyList.of(Fit, FitGz, Tcx, TcxGz, Gpx, GpxGz)

  def fromString(str: String): Either[String, StravaFileType] =
    all.find(_.name.equalsIgnoreCase(str)).toRight(s"Invalid file type: $str")

  def unsafeFromString(str: String): StravaFileType =
    fromString(str).fold(sys.error, identity)
}
