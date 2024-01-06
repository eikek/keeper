package keeper.webview.client.shared

import cats.effect.IO
import fs2.concurrent.Signal

import calico.html.io.{*, given}

object ErrorMessage {

  def list(m: Signal[IO, List[String]], moreCss: Css = Css.none) =
    m.map(bullets =>
      ul(
        cls <-- m.map(lst =>
          Css.errorText + moreCss + Css("list-disc list-inside").showWhen(lst.nonEmpty)
        ),
        bullets.map(li(_))
      )
    )

  def apply(m: Signal[IO, Option[String]], moreCss: Css = Css.none) =
    span(
      cls <-- m.map(errs => moreCss + Css.errorText.showWhen(errs.isDefined)),
      m.map(_.getOrElse(""))
    )
}
