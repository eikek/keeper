package keeper.common

import io.bullet.borer.Encoder

final case class Password(value: String):
  override def toString: String = "***"

object Password:
  given Encoder[Password] = Encoder.forString.contramap(_ => "***")
