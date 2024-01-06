package keeper.core

import cats.{Eq, Order}

import io.bullet.borer.{Decoder, Encoder}

opaque type DeviceId = Long

object DeviceId:
  def apply(n: Long): DeviceId = n

  given Encoder[DeviceId] = Encoder.forLong
  given Decoder[DeviceId] = Decoder.forLong

  given Eq[DeviceId] = Eq.instance((a, b) => a == b)
  given Order[DeviceId] = Order.fromLessThan(_ < _)
  given Ordering[DeviceId] = Order[DeviceId].toOrdering

  extension (self: DeviceId) def asLong: Long = self
