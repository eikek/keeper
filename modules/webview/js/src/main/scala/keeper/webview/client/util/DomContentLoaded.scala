package keeper.webview.client.util

import cats.effect.IO

import keeper.webview.client.cmd.{UiEvent, UiEventDispatch}

import org.scalajs.dom.{DocumentReadyState, Event}

object DomContentLoaded {
  def apply(cr: UiEventDispatch[IO]) =
    fs2.Stream
      .eval(IO {
        org.scalajs.dom.window.document.readyState
      })
      .evalTap(e => IO(scribe.info(s"got document.readyState: $e")))
      .flatMap {
        case DocumentReadyState.loading =>
          fs2.dom
            .events[IO, Event](org.scalajs.dom.window.document, "DOMContentLoaded")
            .evalMap { _ =>
              scribe.info(s"Got DomContentLoaded event!")
              cr.send(UiEvent.DomContentLoaded)
            }

        case _ =>
          fs2.Stream.eval(cr.send(UiEvent.DomContentLoaded))
      }
}
