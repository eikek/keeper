package keeper.webview.client.icons

import cats.effect.{IO, Resource}
import fs2.dom.{HtmlDivElement, HtmlElement}

import keeper.webview.client.shared.Css

import calico.html.Modifier
import calico.html.io.div

abstract class SvgIcon {
  private val defaultCss = Css.fillColor + Css.strokeColor

  protected def content(css: Css): String

  def apply[M](containerMod: M)(svgCss: Css = defaultCss)(using
      M: Modifier[IO, HtmlDivElement[IO], M]
  ): Resource[IO, HtmlElement[IO]] =
    div(containerMod)
      .evalTap(_.innerHtml.set(content(svgCss)))
}
