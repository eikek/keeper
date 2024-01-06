package keeper.core

import cats.{Eq, Order, Show}

import io.bullet.borer.{Decoder, Encoder}

opaque type ComponentId = Long

object ComponentId:
  def apply(n: Long): ComponentId = n

  given Eq[ComponentId] = Eq.fromUniversalEquals
  given Encoder[ComponentId] = Encoder.forLong
  given Decoder[ComponentId] = Decoder.forLong
  given Show[ComponentId] = Show.fromToString
  given Order[ComponentId] = Order.fromLessThan(_ < _)
  given Ordering[ComponentId] = Order[ComponentId].toOrdering

  extension (self: ComponentId) def asLong: Long = self
