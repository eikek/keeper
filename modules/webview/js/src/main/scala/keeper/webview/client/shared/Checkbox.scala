package keeper.webview.client.shared

import cats.effect.IO
import fs2.concurrent.SignallingRef

import keeper.webview.client.util.Action

import calico.html.io.{*, given}

object Checkbox {

  def render(model: SignallingRef[IO, Boolean], label: Option[String]) =
    a(
      cls := Css.flexRowCenter + Css("block cursor-pointer"),
      href := "#",
      onClick --> Action
        .eval(model.update(!_))
        .pipe,
      div(
        cls := "",
        span(
          cls <-- model.changes
            .map(checked =>
              if (checked) Css("fa fa-square-check")
              else Css("fa fa-square font-thin")
            )
        )
      ),
      label.map { txt =>
        div(
          cls := Css("ml-3 py-2"),
          txt
        )
      }
    )
}
