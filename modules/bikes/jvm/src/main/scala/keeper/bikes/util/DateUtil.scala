package keeper.bikes.util

import java.time.*

import cats.Applicative
import cats.effect.Clock
import cats.syntax.all.*

object DateUtil {

  def isCurrent[F[_]: Clock: Applicative](
      ts: Instant,
      delta: Duration = Duration.ofMinutes(10)
  ): F[Boolean] =
    Clock[F].realTimeInstant.map { now =>
      ts.plus(delta).isAfter(now)
    }

  def isNotCurrent[F[_]: Clock: Applicative](
      ts: Instant,
      delta: Duration = Duration.ofMinutes(10)
  ) =
    isCurrent[F](ts, delta).map(flag => !flag)
}
