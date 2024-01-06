package keeper.bikes.data

import cats.data.ValidatedNel
import cats.syntax.all.*
import cats.{Monoid, Show}

import io.bullet.borer.{Decoder, Encoder}

opaque type Weight = Double

object Weight:
  def gramm(g: Int): Weight = g.toDouble
  def gramm(d: Double): Weight = d
  def kg(d: Double): Weight = gramm(d * 1000)

  def fromString(max: Double)(str: String): ValidatedNel[String, Weight] =
    str.toDoubleOption match
      case Some(n) if n > 0 && n < max => gramm(n).validNel[String]
      case Some(n) if n <= 0 =>
        "Negative or empty weights are unfortunately not possible.".invalidNel
      case Some(n) => "That weight is just too much!".invalidNel
      case None    => s"Not a number: $str".invalidNel

  val zero: Weight = 0d

  given Encoder[Weight] = Encoder.forDouble
  given Decoder[Weight] = Decoder.forDouble.map(gramm)

  given Monoid[Weight] = Monoid.instance(zero, _ + _)

  given Show[Weight] =
    Show.show { g =>
      val kg = g / 1000
      if (kg > 1) f"$kg%2fkg"
      else f"$g%.2g"
    }

  extension (self: Weight)
    def +(other: Weight): Weight = gramm(self + other)
    def -(other: Weight): Weight = gramm(self - other)
    def toGramm: Double = self
