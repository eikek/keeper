package keeper.bikes.db

import keeper.common.Password

import com.comcast.ip4s.{Host, Port}
import io.bullet.borer.Encoder
import io.bullet.borer.derivation.MapBasedCodecs.*

final case class PostgresConfig(
    host: Host,
    port: Port,
    database: String,
    user: String,
    password: Password,
    debug: Boolean,
    maxConnections: Int
)

object PostgresConfig:
  given Encoder[Host] = Encoder.forString.contramap(_.toString)
  given Encoder[Port] = Encoder.forInt.contramap(_.value)
  given Encoder[PostgresConfig] = deriveEncoder
