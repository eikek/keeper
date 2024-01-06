package keeper.bikes

import java.time.Instant

import cats.effect.kernel.Async
import cats.effect.{Clock, Resource, Sync}
import cats.syntax.all.*
import cats.{Applicative, Monad}
import fs2.io.net.Network

import keeper.bikes.data.Device
import keeper.bikes.fit4s.{Fit4sConfig, Fit4sDistanceFinder}
import keeper.bikes.model.BikeTotal
import keeper.bikes.strava.StravaDistanceFinder
import keeper.bikes.util.DateUtil
import keeper.strava.StravaService

trait DistanceFinder[F[_]]:
  def findDistanceAt(date: Instant, bikes: List[Device]): F[Option[List[BikeTotal]]]

object DistanceFinder:
  def unsupported[F[_]: Applicative]: DistanceFinder[F] =
    new DistanceFinder[F]:
      def findDistanceAt(
          date: Instant,
          bikes: List[Device]
      ): F[Option[List[BikeTotal]]] =
        Option.empty.pure[F]

  def currentOnly[F[_]: Clock: Monad](
      f: List[Device] => F[List[BikeTotal]]
  ): DistanceFinder[F] =
    new DistanceFinder[F]:
      def findDistanceAt(
          date: Instant,
          bikes: List[Device]
      ): F[Option[List[BikeTotal]]] =
        DateUtil.isCurrent(date).flatMap {
          case true  => f(bikes).map(_.some)
          case false => None.pure[F]
        }

  def findFirst[F[_]: Sync](dfs: Seq[DistanceFinder[F]]): DistanceFinder[F] =
    new DistanceFinder[F]:
      def findDistanceAt(date: Instant, bikes: List[Device]): F[Option[List[BikeTotal]]] =
        fs2.Stream
          .emits(dfs)
          .evalMap(_.findDistanceAt(date, bikes))
          .collectFirst { case Some(list) => list }
          .compile
          .last

  def resource[F[_]: Async: Network](
      fit4sConfig: Option[Fit4sConfig],
      stravaClient: Option[StravaService[F]]
  ): Resource[F, DistanceFinder[F]] =
    for {
      fit4s <- fit4sConfig match
        case None     => Resource.pure(unsupported[F])
        case Some(fc) => Fit4sDistanceFinder.resource(fc)

      strava = stravaClient match
        case Some(cl) => StravaDistanceFinder[F](cl)
        case None     => unsupported[F]
    } yield DistanceFinder.findFirst(List(fit4s, strava))
