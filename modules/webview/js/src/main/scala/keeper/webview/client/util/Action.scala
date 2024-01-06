package keeper.webview.client.util

import cats.effect.IO
import cats.kernel.Monoid
import cats.syntax.all.*
import fs2.dom.{Event, KeyboardEvent}
import fs2.{Pipe, Stream}

import org.scalajs.dom.KeyValue

final case class Action[E](value: E => Stream[IO, Nothing]):
  def +(n: Action[E]): Action[E] =
    Action(e => value(e) ++ n.value(e))

  def pipe: Pipe[IO, E, Nothing] =
    Action.all(this)

object Action:
  def none[E]: Action[E] = Action(_ => Stream.empty)

  def eval[E](fu: IO[Unit]): Action[E] =
    Action(_ => Stream.eval(fu).drain)

  def all[E](action: Action[E]*): Pipe[IO, E, Nothing] =
    _.switchMap(action.combineAll.value)

  def noDefault[E <: Event[IO]]: Action[E] =
    Action(ev => Stream.eval(ev.preventDefault *> ev.stopPropagation).drain)

  given monoid[E]: Monoid[Action[E]] =
    Monoid.instance(none, _ + _)

  extension (self: Action[KeyboardEvent[IO]])
    def onEnter: Pipe[IO, KeyboardEvent[IO], Nothing] =
      in => in.filter(_.key == KeyValue.Enter).through(self.pipe)
