package keeper.bikes

import cats.NonEmptyParallel
import cats.effect.kernel.Resource
import cats.effect.std.Console
import cats.effect.{Async, Temporal}
import cats.syntax.all.*
import fs2.io.file.Files
import fs2.io.net.Network

import keeper.bikes.db.Inventory
import keeper.bikes.db.migration.SchemaMigration
import keeper.bikes.db.postgres.{PostgresInventory, PostgresMaintenanceRepo, SkunkSession}
import keeper.bikes.service.{DefaultBikeServiceBook, DefaultServiceProvider}
import keeper.bikes.strava.StravaService
import keeper.strava.StravaService

import org.typelevel.otel4s.trace.Tracer
import skunk.Session

final class KeeperBikeShop[F[_]](
    val inventory: Inventory[F],
    val serviceProvider: BikeServiceProvider[F],
    val serviceBook: BikeServiceBook[F],
    val stravaService: Option[StravaService[F]]
) extends BikeShop[F]

object KeeperBikeShop:
  def apply[F[_]: Async: Tracer: Network: Console: Temporal: NonEmptyParallel: Files](
      config: Config
  ): Resource[F, BikeShop[F]] =
    SkunkSession(config.database).flatMap(pool => apply(config, pool))

  def apply[F[_]: Async: Network: NonEmptyParallel: Files](
      config: Config,
      pool: Resource[F, Session[F]]
  ): Resource[F, BikeShop[F]] =
    for {
      strava <- config.stravaConfig
        .map(cfg => StravaService.resource(cfg, pool).map(_.some))
        .getOrElse(Resource.pure(None))

      df <- DistanceFinder.resource[F](config.fit4sConfig, strava)

      _ <- Resource.eval(pool.use(s => SchemaMigration[F](s).migrate))

      shop <- Resource.pure {
        val inventory = new PostgresInventory[F](pool)
        val maintRepo = new PostgresMaintenanceRepo[F](pool)
        val serviceProvider = new DefaultServiceProvider[F](inventory, maintRepo)
        val serviceBook = new DefaultBikeServiceBook[F](inventory, maintRepo, df)
        new KeeperBikeShop[F](inventory, serviceProvider, serviceBook, strava)
      }
    } yield shop
