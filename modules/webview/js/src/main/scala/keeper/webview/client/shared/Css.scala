package keeper.webview.client.shared

import cats.effect.IO
import cats.kernel.Monoid
import cats.syntax.all.*
import fs2.concurrent.Signal

import calico.html.ClassProp

case class Css(styles: List[String]) {
  def +(other: Css): Css = Css(styles ++ other.styles)

  def showWhen(flag: Boolean): Css =
    if (flag) this else Css.hidden

  def visibleWhen(flag: Boolean): Css =
    if (flag) this else Css("invisible")

  def when(flag: Boolean): Css =
    if (flag) this else Css.none

  def render: String = styles.mkString(" ")
}
object Css extends Styles {
  val none: Css = Css(Nil)

  given Monoid[Css] = Monoid.instance(none, _ + _)

  def apply(styles: String*): Css =
    Css(styles.toList)

  def of(more: Css*): Css = more.combineAll

  extension (p: ClassProp[IO])
    def :=(css: Css) = p.:=(css.styles)
    def <--(css: Signal[IO, Css]) = p <-- css.map(_.styles)

}
