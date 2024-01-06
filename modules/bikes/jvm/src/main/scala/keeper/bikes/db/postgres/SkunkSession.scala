package keeper.bikes.db.postgres

import cats.effect.*
import cats.effect.std.Console
import cats.syntax.all.*
import fs2.io.net.Network

import keeper.bikes.db.PostgresConfig

import org.typelevel.otel4s.trace.Tracer
import skunk.Session

object SkunkSession {

  def apply[F[_]: Tracer: Network: Console: Temporal](
      cfg: PostgresConfig
  ): Resource[F, Resource[F, Session[F]]] =
    if (cfg.maxConnections <= 1)
      Resource.pure(
        Session.single(
          host = cfg.host.toString,
          port = cfg.port.value,
          user = cfg.user,
          database = cfg.database,
          password = cfg.password.value.some,
          debug = cfg.debug
        )
      )
    else
      Session
        .pooled[F](
          host = cfg.host.toString,
          port = cfg.port.value,
          user = cfg.user,
          database = cfg.database,
          password = cfg.password.value.some,
          debug = cfg.debug,
          max = cfg.maxConnections
        )
}
