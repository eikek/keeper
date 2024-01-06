package keeper.bikes

import cats.NonEmptyParallel
import cats.effect.std.Console
import cats.effect.{Async, Resource, Temporal}
import fs2.io.file.Files
import fs2.io.net.Network

import keeper.bikes.db.Inventory
import keeper.strava.StravaService

import org.typelevel.otel4s.trace.Tracer

trait BikeShop[F[_]] {

  def inventory: Inventory[F]

  def serviceProvider: BikeServiceProvider[F]

  def serviceBook: BikeServiceBook[F]

  def stravaService: Option[StravaService[F]]
}

object BikeShop:
  def resource[F[_]: Tracer: Network: Console: Temporal: Async: NonEmptyParallel: Files](
      cfg: Config
  ): Resource[F, BikeShop[F]] = KeeperBikeShop[F](cfg)
