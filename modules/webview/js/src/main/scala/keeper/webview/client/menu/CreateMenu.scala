package keeper.webview.client.menu

import cats.effect.{IO, Resource}
import fs2.concurrent.SignallingRef
import fs2.dom.HtmlElement

import keeper.webview.client.View
import keeper.webview.client.cmd.{UiEvent, UiEventDispatch}
import keeper.webview.client.shared.Css
import keeper.webview.client.util.Action

import calico.html.io.{*, given}

object CreateMenu {
  final case class Model(
      open: Boolean
  ):
    def toggleOpen: Model = copy(open = !this.open)

  object Model:
    val empty: Model = Model(false)
    def emptyRef: IO[SignallingRef[IO, Model]] = SignallingRef.of(empty)

  private val menuStyle: Css =
    Css(
      "flex flex-col w-64 absolute top-0 right-0 dark:bg-slate-950 bg-gray-200 items-end"
    )

  private val menuLinkStyle: Css =
    Css(
      "rounded w-full px-4 py-4 dark:hover:text-slate-100 hover:text-gray-900 cursor-pointer"
    )

  private def menuLink(model: SignallingRef[IO, Model]) =
    a(
      cls := Css.of(
        Css("inline-flex items-center cursor-pointer"),
        Css("h-16 w-16 px-4 font-bold text-2xl"),
        Css.textColorViz,
        Css.hoverBgBorderColor
      ),
      onClick --> Action.eval(model.update(_.toggleOpen)).pipe,
      i(cls := "fa fa-plus mx-auto")
    )

  private def menuEntry(
      name: String,
      icon: String,
      page: View,
      model: SignallingRef[IO, Model],
      cr: UiEventDispatch[IO]
  ) =
    a(
      cls := menuLinkStyle,
      i(cls := Css(s"fa $icon mr-2")),
      onClick --> Action.all(
        Action.eval(cr.send(UiEvent.SetView(page))),
        Action.eval(model.update(_.toggleOpen))
      ),
      name
    )

  def render(
      model: SignallingRef[IO, Model],
      cr: UiEventDispatch[IO]
  ): Resource[IO, HtmlElement[IO]] =
    div(
      cls := Css.flexRowCenter + Css("mr-2 relative z-40"),
      menuLink(model),
      div(
        cls <-- model.map(m => menuStyle.showWhen(m.open)),
        div(
          cls := Css.flexRowCenter,
          menuLink(model)
        ),
        menuEntry("New Bike", "fa-bicycle", View.NewBike, model, cr),
        menuEntry("New Service", "fa-screwdriver-wrench", View.NewMaintenance, model, cr)
      )
    )
}
