package keeper.webview.client.dashboard

import java.time.{Instant, ZoneId}

import cats.Eq
import cats.effect.*
import cats.syntax.all.*
import fs2.concurrent.{Signal, SignallingRef}

import keeper.bikes.model.{BikeBuilds, ServiceSearchMask}
import keeper.client.KeeperClient
import keeper.client.data.FetchResult
import keeper.common.Lenses.syntax.*
import keeper.webview.client.cmd.*
import keeper.webview.client.icons.ComponentIcon
import keeper.webview.client.shared.Css
import keeper.webview.client.util.Action

import calico.html.io.{*, given}

object DashboardPage {
  private[this] val logger = scribe.cats.io

  def render(
      client: KeeperClient[IO],
      model: SignallingRef[IO, Model],
      date: Signal[IO, Option[Instant]],
      cr: UiEventDispatch[IO],
      zone: ZoneId
  ) =
    getBikes(client, date, model) >>
      getServices(client, date, model) >>
      div(
        cls := Css.flexRowXl + Css("px-4"),
        div(
          cls := Css.flexCol + Css("w-full xl:w-1/3"),
          model.map(_.bikes.bikes).changes.map { bikes =>
            div(
              cls := Css.flexCol + Css("mt-4 space-y-2"),
              bikes.map(b =>
                BikeDiv(
                  b,
                  model.to(Model.detailFor(b.id)),
                  model.to(Model.totalsFor(b.id)),
                  model.to(Model.componentTotals)
                )
              )
            )
          }
        ),
        div(
          cls := Css.flexCol + Css("grow xl:container"),
          div(
            cls := Css.of(
              Css.flexRowCenter,
              Css.borderB,
              Css("py-2 mx-2 text-2xl mt-4"),
              Css.textColorViz
            ),
            div(cls := "fa fa-receipt mr-2"),
            div(
              cls := Css("grow"),
              "Maintenance Log"
            ),
            renderFilter(model.to(Model.searchMask))
          ),
          ServiceList.render(model.map(_.serviceList), model.to(Model.searchMask), zone)
        )
      )

  def renderFilter(search: SignallingRef[IO, SearchMaskModel]) =
    div(
      cls <-- search
        .map(_.nonEmpty)
        .map((Css("space-x-1 text-base") + Css.flexRowCenter).showWhen),
      search
        .map(_.searchBikes.toList.sortBy(_.name))
        .changes
        .map(bikes =>
          div(
            cls := Css.flexRowCenter + Css("space-x-1"),
            bikes.map { b =>
              a(
                cls := Css.labelBasic,
                href := "#",
                onClick --> Action
                  .eval(search.update(SearchMaskModel.searchBikesAt(b).replace(false)))
                  .pipe,
                span(cls := "mr-2", b.name),
                span(cls := "fa fa-times")
              )
            }
          )
        ),
      search
        .map(_.searchComps.toList.sortBy(_.name))
        .changes
        .map(comps =>
          div(
            cls := Css.flexRowCenter + Css("space-x-1"),
            comps.map { c =>
              a(
                cls := Css.labelBasic + Css.flexRowCenter,
                href := "#",
                onClick --> Action
                  .eval(search.update(SearchMaskModel.searchCompsAt(c).replace(false)))
                  .pipe,
                ComponentIcon(
                  c.typ,
                  cls := "h-5 w-5 mr-1",
                  Css("dark:fill-slate-300 dark:stroke-slate-300")
                ),
                div(cls := "mr-2", c.name),
                div(cls := "fa fa-times")
              )
            }
          )
        ),
      search
        .map(_.searchActions.toList.sortBy(_.asString))
        .changes
        .map(acts =>
          div(
            cls := Css.flexRowCenter + Css("space-x-1"),
            acts.map { c =>
              a(
                cls := Css.labelBasic,
                href := "#",
                onClick --> Action
                  .eval(search.update(SearchMaskModel.searchActionAt(c).replace(false)))
                  .pipe,
                span(cls := "mr-2", c.asString),
                span(cls := "fa fa-times")
              )
            }
          )
        )
    )

  def getServices(
      client: KeeperClient[IO],
      selectedDate: Signal[IO, Option[Instant]],
      model: SignallingRef[IO, Model]
  ) =
    (
      selectedDate,
      model.map(_.searchMaskModel.toSearch)
    )
      .mapN(ServiceSearchMask.untilDate.replace(_)(_))
      .changes
      .discrete
      .evalMap { mask =>
        client.getServiceDetails(mask)
      }
      .evalMap {
        case FetchResult.Success(list) =>
          model.update(Model.serviceList.replace(list))
        case FetchResult.RequestFailed(err) =>
          logger.error(s"Error getting service list: $err")
      }
      .compile
      .drain
      .background

  def getBikes(
      client: KeeperClient[IO],
      selectedDate: Signal[IO, Option[Instant]],
      model: SignallingRef[IO, Model]
  ): Resource[IO, Unit] =
    selectedDate
      .changes(Eq.fromUniversalEquals)
      .discrete
      .evalMap {
        case Some(d) => client.getBikesAt(d)
        case None    => client.getCurrentBikes
      }
      .evalMap {
        case FetchResult.Success(r) => model.update(Model.bikes.replace(r))
        case FetchResult.RequestFailed(err) =>
          logger.error(s"get bikes failed: $err")
      }
      .compile
      .drain
      .background
      .void
}
