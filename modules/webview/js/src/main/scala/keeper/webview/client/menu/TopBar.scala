package keeper.webview.client.menu

import java.time.{Instant, LocalDate, ZoneId}

import cats.effect.{IO, Resource}
import cats.syntax.all.*
import fs2.concurrent.SignallingRef
import fs2.dom.HtmlDivElement

import keeper.common.Lenses
import keeper.common.Lenses.syntax.*
import keeper.webview.client.View
import keeper.webview.client.cmd.{UiEvent, UiEventDispatch}
import keeper.webview.client.icons.Bicycle
import keeper.webview.client.shared.Css
import keeper.webview.client.util.{Action, DateTime}

import calico.html.io.{*, given}
import monocle.Lens

object TopBar:
  final case class Model(
      menu: Menu.Model,
      create: CreateMenu.Model,
      currentDate: String,
      date: Option[String]
  ):
    def dateValidated(zoneId: ZoneId): Option[Instant] =
      date
        .traverse(DateTime.parseDate)
        .map(_.map(DateTime.atEndOfDay(zoneId)).map(_.toInstant))
        .toOption
        .flatten

    def dateOrCurrent = date.getOrElse(currentDate)

  object Model:
    val empty: Model = Model(Menu.Model.empty, CreateMenu.Model.empty, "", None)
    val menu: Lens[Model, Menu.Model] =
      Lens[Model, Menu.Model](_.menu)(a => _.copy(menu = a))
    val create: Lens[Model, CreateMenu.Model] =
      Lens[Model, CreateMenu.Model](_.create)(a => _.copy(create = a))
    val date: Lens[Model, String] =
      Lens[Model, String](_.dateOrCurrent)(a =>
        _.copy(date = Option(a).filter(_.nonEmpty))
      )
    val currentDate: Lens[Model, String] =
      Lens[Model, String](_.currentDate)(a => _.copy(currentDate = a))

  def render(
      model: SignallingRef[IO, Model],
      cr: UiEventDispatch[IO],
      zoneId: ZoneId
  ): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := Css.of(
        Css.flexRowCenter,
        Css.borderB2,
        Css("h-16 dark:bg-slate-950 bg-gray-50")
      ),
      Menu.render(model.to(Model.menu), cr),
      a(
        cls := Css("font-bold text-2xl") + Css.flexRowCenter + Css.textColorViz,
        href := "#",
        onClick --> Action.eval(cr.send(UiEvent.SetView(View.Dashboard))).pipe,
        Bicycle(cls := "w-10 h-10 mr-2")(Css("stroke-blue-500 fill-blue-500")),
        div(
          cls := "hidden md:block",
          "My Bikes"
        )
      ),
      div(
        cls := Css("grow")
      ),
      renderDateInput(model, zoneId),
      div(
        cls := Css.none,
        CreateMenu.render(model.to(Model.create), cr)
      )
    )

  def renderDateInput(model: SignallingRef[IO, Model], zoneId: ZoneId) =
    setCurrentDate(model, zoneId).toResource >>
      div(
        cls := Css.flexRowCenter,
        div(
          cls := Css("py-1"),
          input.withSelf { dateIn =>
            (
              cls := Css.textInput,
              typ := "date",
              value <-- model.map(_.dateOrCurrent),
              onInput --> Action
                .eval(dateIn.value.get.flatMap(model.to(Model.date).set))
                .pipe
            )
          }
        ),
        div(
          cls := Css.iconLinkBasic + Css("ml-1"),
          a(
            href := "#",
            span(cls := "fa fa-times"),
            onClick --> Action
              .eval(setCurrentDate(model, zoneId) >> model.to(Model.date).set(""))
              .pipe
          )
        )
      )

  def setCurrentDate(model: SignallingRef[IO, Model], zoneId: ZoneId) =
    IO(LocalDate.now(zoneId)).map(_.toString).flatMap(model.to(Model.currentDate).set)
