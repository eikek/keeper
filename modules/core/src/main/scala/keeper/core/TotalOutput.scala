package keeper.core

import cats.kernel.Monoid

import keeper.common.Distance

import io.bullet.borer.{Decoder, Encoder}

/** Some number indicating the total usage of a device. This can be the mileage of a car
  * or bike, or the number of shot servings of a coffee machine.
  */
opaque type TotalOutput = Double

object TotalOutput:
  def apply(n: Double): TotalOutput = math.max(zero, n)

  def fromDistance(d: Distance): TotalOutput = d.toMeter

  val zero: TotalOutput = 0d

  def fromString(str: String): Either[String, TotalOutput] =
    str.toDoubleOption match
      case Some(d) if d >= 0 => Right(d)
      case Some(d)           => Left(s"Negative totals are not possible.")
      case None              => Left(s"Not a number for total output: $str")

  given Encoder[TotalOutput] = Encoder.forDouble
  given Decoder[TotalOutput] = Decoder.forDouble.map(apply)

  given Monoid[TotalOutput] = Monoid.instance(zero, _ + _)

  extension (self: TotalOutput)
    def +(other: TotalOutput): TotalOutput = TotalOutput(self + other)
    def -(other: TotalOutput): TotalOutput = TotalOutput(self - other)
    def *(factor: Double): TotalOutput = self * factor
    def >(n: TotalOutput): Boolean = self > n
    def >=(n: TotalOutput): Boolean = self >= n
    def <(n: TotalOutput): Boolean = self < n
    def <=(n: TotalOutput): Boolean = self <= n
    def isPositive: Boolean = self > 0
    def asDouble: Double = self
    def positiveOrZero: TotalOutput = if (self >= 0) self else 0
    def toDistance: Distance = Distance.meter(self)
