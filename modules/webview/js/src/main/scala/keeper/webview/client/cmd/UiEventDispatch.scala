package keeper.webview.client.cmd

import cats.Parallel
import cats.effect.*
import cats.syntax.all.*
import fs2.Stream
import fs2.concurrent.Topic

trait UiEventDispatch[F[_]] {
  def send(event: UiEvent): F[Unit]

  def subscribe: Stream[F, UiEvent]
}

object UiEventDispatch {
  def apply[F[_]: Async: Parallel]: F[UiEventDispatch[F]] =
    Topic[F, UiEvent].map(t => new Impl[F](t))

  final private class Impl[F[_]: Async: Parallel](
      topic: Topic[F, UiEvent]
  ) extends UiEventDispatch[F] {
    override def subscribe: Stream[F, UiEvent] = topic.subscribeUnbounded.changes
    override def send(event: UiEvent): F[Unit] =
      topic.publish1(event).void
  }
}
