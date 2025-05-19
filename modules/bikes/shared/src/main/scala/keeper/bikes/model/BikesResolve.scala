package keeper.bikes.model

import java.time.Instant

import cats.Monad
import cats.data.{EitherT, NonEmptyList}
import cats.syntax.all.*

import keeper.bikes.data.{ComponentType, ComponentWithProduct}
import keeper.core.DeviceBuild

final class BikesResolve[F[_]: Monad](source: ComponentSource[F]) {
  private type Result[A] = EitherT[F, BikesResolveError, A]

  def resolve(
      builds: DeviceBuild,
      at: Instant
  ): F[Either[BikesResolveError, BikeBuilds]] =
    resolveT(builds, at).value

  def resolveT(builds: DeviceBuild, at: Instant): Result[BikeBuilds] =
    for {
      resolvedBuild <- ResolvedBuild.load[F](source, builds, at)
      bikes = resolvedBuild.getBikes.map(b =>
        val mainComponents = resolvedBuild.getTopLevel(b.id)
        resolveBike(resolvedBuild, mainComponents)(b)
      )
      rws <- EitherT.right(allRearWheels(at, resolvedBuild))
      fws <- EitherT.right(allFrontWheels(at, resolvedBuild))
      forks <- EitherT.right(allForks(at, resolvedBuild))
    } yield BikeBuilds(bikes, rws.toList, fws.toList, forks.toList, Nil, Map.empty)

  private def allForks(at: Instant, resolvedBuild: ResolvedBuild) =
    allConfiguredComponents(
      at,
      resolvedBuild,
      ComponentType.Fork,
      Fork.fromComponent(_),
      resolveFork(resolvedBuild, _)
    )

  private def allRearWheels(at: Instant, build: ResolvedBuild) =
    allConfiguredComponents(
      at,
      build,
      ComponentType.RearWheel,
      RearWheel.fromComponent(_),
      resolveRearWheel
    )

  private def allFrontWheels(at: Instant, build: ResolvedBuild) =
    allConfiguredComponents(
      at,
      build,
      ComponentType.FrontWheel,
      FrontWheel.fromComponent(_),
      resolveFrontWheel
    )

  private def allConfiguredComponents[A <: BikePart](
      at: Instant,
      build: ResolvedBuild,
      ct: ComponentType,
      from: ComponentWithProduct => A,
      populate: List[ComponentWithProduct] => A => A
  ): F[Seq[A]] =
    for {
      rws <- source.getComponentsOfType(at, NonEmptyList.of(ct))
      ws = rws.map(from)
      result = ws.map { w =>
        val subs = build.getSubComponents(w.id)
        populate(subs).apply(w)
      }
    } yield result

  private def resolveBike(
      build: ResolvedBuild,
      comps: List[ComponentWithProduct]
  ): Bike => Bike =
    comps.foldLeft(identity[Bike]) { (b, c) =>
      b.andThen {
        c.product.productType match
          case ComponentType.FrontWheel =>
            Bike.frontWheel.replace(
              resolveFrontWheel(build.getSubComponents(c.id))(
                FrontWheel.fromComponent(c)
              ).some
            )
          case ComponentType.RearWheel =>
            Bike.rearWheel.replace(
              resolveRearWheel(build.getSubComponents(c.id))(
                RearWheel.fromComponent(c)
              ).some
            )

          case ComponentType.Handlebar =>
            Bike.handlebar.replace(BasicComponent.fromComponent(c).some)

          case ComponentType.Seatpost =>
            Bike.seatpost.replace(BasicComponent.fromComponent(c).some)

          case ComponentType.Saddle =>
            Bike.saddle.replace(BasicComponent.fromComponent(c).some)

          case ComponentType.Stem =>
            Bike.stem.replace(BasicComponent.fromComponent(c).some)

          case ComponentType.Chain =>
            Bike.chain.replace(BasicComponent.fromComponent(c).some)

          case ComponentType.CrankSet =>
            Bike.crankSet.replace(BasicComponent.fromComponent(c).some)

          case ComponentType.RearBrake =>
            Bike.rearBrake.replace(
              resolveBrakeCaliper(build.getSubComponents(c.id))(
                BrakeCaliper.fromComponent(c)
              ).some
            )

          case ComponentType.Fork =>
            Bike.fork.replace(
              resolveFork(build, build.getSubComponents(c.id))(
                Fork.fromComponent(c)
              ).some
            )

          case ComponentType.FrontDerailleur =>
            Bike.frontDerailleur.replace(BasicComponent.fromComponent(c).some)

          case ComponentType.RearDerailleur =>
            Bike.rearDerailleur.replace(BasicComponent.fromComponent(c).some)

          case ComponentType.RearMudguard =>
            Bike.rearMudguard.replace(BasicComponent.fromComponent(c).some)

          case _ =>
            identity
      }
    }

  private def resolveFork(
      build: ResolvedBuild,
      comps: List[ComponentWithProduct]
  ): Fork => Fork =
    comps.foldLeft(identity[Fork]) { (f, c) =>
      f.andThen {
        c.product.productType match
          case ComponentType.FrontBrake =>
            Fork.brakeCaliper.replace(
              resolveBrakeCaliper(build.getSubComponents(c.id))(
                BrakeCaliper.fromComponent(c)
              ).some
            )
          case ComponentType.FrontMudguard =>
            Fork.mudguard.replace(BasicComponent.fromComponent(c).some)
          case _ =>
            f
      }
    }

  private def resolveBrakeCaliper(
      comps: List[ComponentWithProduct]
  ): BrakeCaliper => BrakeCaliper =
    comps.foldLeft(identity[BrakeCaliper]) { (f, c) =>
      c.product.productType match
        case ComponentType.BrakePad =>
          f.andThen(BrakeCaliper.pad.replace(BasicComponent.fromComponent(c).some))
        case _ =>
          f
    }

  private def resolveFrontWheel(
      comps: List[ComponentWithProduct]
  ): FrontWheel => FrontWheel =
    comps.foldLeft(identity[FrontWheel]) { (f, c) =>
      c.product.productType match
        case ComponentType.Tire =>
          f.andThen(FrontWheel.tire.replace(BasicComponent.fromComponent(c).some))
        case ComponentType.BrakeDisc =>
          f.andThen(FrontWheel.brakeDisc.replace(BasicComponent.fromComponent(c).some))
        case ComponentType.InnerTube =>
          f.andThen(FrontWheel.tube.replace(BasicComponent.fromComponent(c).some))
        case _ =>
          f
    }

  private def resolveRearWheel(
      comps: List[ComponentWithProduct]
  ): RearWheel => RearWheel =
    comps.foldLeft(identity[RearWheel]) { (f, c) =>
      c.product.productType match
        case ComponentType.Tire =>
          f.andThen(RearWheel.tire.replace(BasicComponent.fromComponent(c).some))
        case ComponentType.BrakeDisc =>
          f.andThen(RearWheel.brakeDisc.replace(BasicComponent.fromComponent(c).some))
        case ComponentType.InnerTube =>
          f.andThen(RearWheel.tube.replace(BasicComponent.fromComponent(c).some))
        case ComponentType.Cassette =>
          f.andThen(RearWheel.cassette.replace(BasicComponent.fromComponent(c).some))
        case _ =>
          f
    }
}
