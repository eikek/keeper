package keeper.bikes.db.postgres

import cats.{Eq, Order, Show}

import io.bullet.borer.{Decoder, Encoder}

opaque type ServiceEventId = Long

object ServiceEventId:

  def apply(n: Long): ServiceEventId = n

  given Eq[ServiceEventId] = Eq.fromUniversalEquals
  given Encoder[ServiceEventId] = Encoder.forLong
  given Decoder[ServiceEventId] = Decoder.forLong
  given Show[ServiceEventId] = Show.fromToString
  given Order[ServiceEventId] = Order.fromLessThan(_ < _)

  extension (self: ServiceEventId) def asLong: Long = self
