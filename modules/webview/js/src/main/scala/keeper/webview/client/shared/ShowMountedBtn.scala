package keeper.webview.client.shared

import cats.effect.{IO, Resource}
import fs2.concurrent.SignallingRef
import fs2.dom.HtmlAnchorElement

import keeper.webview.client.util.Action

import calico.html.io.{*, given}

object ShowMountedBtn {

  def render(model: SignallingRef[IO, Boolean]): Resource[IO, HtmlAnchorElement[IO]] =
    a(
      cls := Css.formResetButton,
      href := "#",
      model.changes.map {
        case true  => "Hide mounted parts"
        case false => "Show mounted parts"
      },
      onClick --> Action
        .eval(model.update(!_))
        .pipe
    )
}
