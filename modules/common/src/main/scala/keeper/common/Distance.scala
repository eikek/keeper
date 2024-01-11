package keeper.common

import cats.{Eq, Monoid, Show}

import io.bullet.borer.{Decoder, Encoder}

opaque type Distance = Double

object Distance:
  val zero: Distance = 0d

  def meter(n: Double): Distance = n
  def km(n: Double): Distance = n * 1000d
  def mile(n: Double): Distance = n * 1609.344

  def fromString(str: String): Either[String, Distance] =
    val lcs = str.toLowerCase
    def asDouble(s: String) =
      s.toDoubleOption.toRight(s"Invalid distance: $str")
    if (lcs.endsWith("km")) asDouble(lcs.dropRight(2)).map(km)
    else if (lcs.endsWith("m")) asDouble(lcs.dropRight(1)).map(meter)
    else asDouble(lcs).map(km)

  given Show[Distance] =
    Show.show { m =>
      if (m.toKm > 1) f"${m.toKm}%.2fkm"
      else f"$m%.2fm"
    }

  given Monoid[Distance] =
    Monoid.instance(zero, _ + _)

  given Encoder[Distance] = Encoder.forDouble
  given Decoder[Distance] = Decoder.forDouble
  given Eq[Distance] = Eq.fromUniversalEquals

  extension (self: Distance)
    def toMeter: Double = self
    def toKm: Double = self / 1000d
    def toMile: Double = self / 1609.344

    def *(factor: Double): Distance = self * factor
    def +(other: Distance): Distance = self + other
    def -(other: Distance): Distance = self - other
    def >(other: Distance): Boolean = self > other
