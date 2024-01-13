package keeper.cli.config

import io.bullet.borer.Encoder
import io.bullet.borer.derivation.MapBasedCodecs.deriveEncoder
import scribe.Level

final case class LoggingConfig(
    minimumLevel: Level
)

object LoggingConfig:
  given Encoder[Level] = Encoder.forString.contramap(_.name)
  given Encoder[LoggingConfig] = deriveEncoder
