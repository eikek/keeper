package keeper.bikes.model

import java.time.Instant

import cats.data.NonEmptyList
import cats.syntax.all.*
import cats.{Applicative, Monad, Monoid}

import keeper.bikes.data.{ComponentType, ComponentWithProduct, DeviceWithBrand}
import keeper.core.{ComponentId, DeviceId}

trait ComponentSource[F[_]] { self =>

  def findDevice(id: DeviceId, at: Instant): F[Option[DeviceWithBrand]]

  def findComponent(id: ComponentId, at: Instant): F[Option[ComponentWithProduct]]

  def getComponentsOfType(
      at: Instant,
      cts: NonEmptyList[ComponentType]
  ): F[Seq[ComponentWithProduct]]

  def andThen(next: ComponentSource[F])(using Monad[F]): ComponentSource[F] =
    new ComponentSource[F]:
      def findDevice(id: DeviceId, at: Instant): F[Option[DeviceWithBrand]] =
        self.findDevice(id, at).flatMap {
          case d @ Some(_) => d.pure[F]
          case None        => next.findDevice(id, at)
        }

      def findComponent(id: ComponentId, at: Instant): F[Option[ComponentWithProduct]] =
        self.findComponent(id, at).flatMap {
          case c @ Some(_) => c.pure[F]
          case None        => next.findComponent(id, at)
        }

      def getComponentsOfType(
          at: Instant,
          cts: NonEmptyList[ComponentType]
      ): F[Seq[ComponentWithProduct]] =
        (self.getComponentsOfType(at, cts), next.getComponentsOfType(at, cts))
          .mapN(_ ++ _)
}

object ComponentSource:
  def empty[F[_]: Applicative]: ComponentSource[F] =
    new ComponentSource[F]:
      def findDevice(id: DeviceId, at: Instant): F[Option[DeviceWithBrand]] =
        Option.empty.pure[F]
      def findComponent(id: ComponentId, at: Instant): F[Option[ComponentWithProduct]] =
        Option.empty.pure[F]
      def getComponentsOfType(
          at: Instant,
          cts: NonEmptyList[ComponentType]
      ): F[Seq[ComponentWithProduct]] =
        Seq.empty.pure[F]

  def from[F[_]: Applicative](
      fd: DeviceId => Option[DeviceWithBrand],
      fc: ComponentId => Option[ComponentWithProduct],
      ft: NonEmptyList[ComponentType] => Seq[ComponentWithProduct]
  ): ComponentSource[F] =
    new ComponentSource[F]:
      def findDevice(id: DeviceId, at: Instant): F[Option[DeviceWithBrand]] =
        fd(id).pure[F]
      def findComponent(id: ComponentId, at: Instant): F[Option[ComponentWithProduct]] =
        fc(id).pure[F]
      def getComponentsOfType(
          at: Instant,
          cts: NonEmptyList[ComponentType]
      ): F[Seq[ComponentWithProduct]] =
        ft(cts).pure[F]

  given componentSourceMonoid[F[_]: Monad]: Monoid[ComponentSource[F]] =
    Monoid.instance(empty[F], _ andThen _)
