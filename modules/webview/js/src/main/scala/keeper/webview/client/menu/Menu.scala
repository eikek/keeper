package keeper.webview.client.menu

import cats.effect.{IO, Resource}
import fs2.concurrent.SignallingRef
import fs2.dom.HtmlElement

import keeper.webview.client.View
import keeper.webview.client.cmd.{UiEvent, UiEventDispatch}
import keeper.webview.client.shared.Css
import keeper.webview.client.util.Action

import calico.html.io.{*, given}

object Menu {

  final case class Model(
      open: Boolean
  ):
    def toggleOpen: Model = copy(open = !this.open)

  object Model:
    val empty: Model = Model(false)
    def emptyRef: IO[SignallingRef[IO, Model]] = SignallingRef.of(empty)

  private val menuStyle: Css =
    Css(
      "flex flex-col w-64 h-screen absolute top-0 left-0 dark:bg-slate-950 bg-gray-200"
    )

  private val menuLinkStyle: Css =
    Css(
      "rounded-lg px-2 py-2 my-1 mx-4 dark:hover:text-slate-100 hover:text-gray-900 cursor-pointer"
    ) + Css("dark:hover:bg-slate-600")

  private def menuLink(model: SignallingRef[IO, Model]) =
    a(
      cls := Css.of(
        Css("inline-flex items-center cursor-pointer"),
        Css("h-16 w-16 px-4 font-bold text-2xl"),
        Css.textColorViz,
        Css.hoverBgBorderColor
      ),
      onClick --> Action.eval(model.update(_.toggleOpen)).pipe,
      i(cls := "fa fa-bars mx-auto")
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

  private def subMenu(
      icon: Option[String],
      title: String,
      entries: Resource[IO, HtmlElement[IO]]*
  ) =
    div(
      cls := Css.flexCol + Css("px-2 mt-3"),
      div(
        cls := Css("italic text-lg") + Css.textColorViz,
        icon.map(i => span(cls := Css(s"$i mr-2"))),
        title
      ),
      entries.toList
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
          cls := Css.flexRowCenter + Css("mb-4"),
          menuLink(model)
        ),
        menuEntry("Dashboard", "fa-tachometer", View.Dashboard, model, cr),
        subMenu(
          Some("fa fa-warehouse"),
          "Inventory",
          menuEntry("Add components", "fa-puzzle-piece", View.Components, model, cr)
        ),
        subMenu(
          Some("fa fa-boxes-stacked"),
          "Admin",
          menuEntry("Products", "fa-shopping-basket", View.Products, model, cr),
          menuEntry("Brands", "fa-dollar-sign", View.Brands, model, cr),
          menuEntry("Setup Strava", "fa-brands fa-strava", View.StravaSetup, model, cr)
        )
      )
    )
}
