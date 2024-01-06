package keeper.webview.client.components

import java.time.{Instant, ZoneId}

import cats.effect.{IO, Resource}
import cats.syntax.all.*
import fs2.concurrent.SignallingRef
import fs2.dom.HtmlDivElement

import keeper.bikes.*
import keeper.client.KeeperClient
import keeper.client.data.FetchResult
import keeper.webview.client.cmd.*
import keeper.webview.client.icons.ComponentIcon
import keeper.webview.client.shared.Css
import keeper.webview.client.util.{Action, FormatDate}

import calico.html.io.{*, given}
import org.scalajs.dom.KeyValue

object ComponentTable {
  private[this] val logger = scribe.cats.io

  def render(
      model: SignallingRef[IO, TableModel],
      client: KeeperClient[IO],
      dispatch: UiEventDispatch[IO],
      zoneId: ZoneId
  ): Resource[IO, HtmlDivElement[IO]] =
    subscribe(model, client)(dispatch).compile.drain.background >>
      doSearch(model, client).toResource >>
      getBikes(model, client).toResource >>
      div(
        cls := Css.flexCol + Css("mt-8 w-full"),
        div(
          cls := Css.flexRowCenter + Css("mb-2"),
          input.withSelf { self =>
            (
              cls := Css.textInput,
              placeholder := "Search components…",
              autoFocus := true,
              onKeyDown --> {
                _.filter(_.key == KeyValue.Enter)
                  .evalMap(_ => self.value.get)
                  .foreach(str =>
                    model.updateAndGet(TableModel.queryString.replace(str)) >> doSearch(
                      model,
                      client
                    )
                  )
              }
            )
          }
        ),
        table(
          cls := Css.tableFixed,
          thead(
            cls := Css.tableHead + Css("sticky top-0"),
            tr(
              cls := Css.tableHeadRow,
              th(cls := Css.tableHeadCell + Css("w-1/12")),
              th(cls := Css.tableHeadCell + Css("w-5/12"), "Name"),
              th(cls := Css.tableHeadCellMd + Css("w-2/12"), "Total"),
              th(cls := Css.tableHeadCellMd + Css("w-2/12"), "Added"),
              th(cls := Css.tableHeadCell + Css("w-4/12 md:w-3/12"), "")
            )
          ),
          model
            .map(_.componentsWithTotals)
            .changes
            .map(cs =>
              tbody(
                cls := "overflow-y-auto",
                cs.map { case (p, dst) =>
                  tr(
                    cls := Css.tableRow,
                    td(
                      cls := Css.tableRowCell,
                      ComponentIcon(
                        p.product.productType,
                        cls := "h-8 w-8 mx-auto"
                      )
                    ),
                    td(
                      cls := Css.tableRowCell,
                      span(p.component.name),
                      span(
                        cls := Css("ml-2 text-xs"),
                        "(",
                        p.brand.name,
                        " ",
                        p.product.name,
                        ")"
                      )
                    ),
                    td(
                      cls := Css.tableRowCellMd,
                      dst.map(_.show).getOrElse("")
                    ),
                    td(
                      cls := Css.tableRowCellMd,
                      FormatDate(p.component.addedAt, zoneId),
                      p.component.removedAt.map { removed =>
                        div(
                          cls := "ml-2 text-red-400",
                          span(cls := "fa fa-skull-crossbones mr-1"),
                          FormatDate(removed, zoneId)
                        )
                      }
                    ),
                    td(
                      cls := Css.tableRowCell + Css.flexRowCenter + Css("justify-end"),
                      a(
                        cls := Css.iconLink + Css("mr-2"),
                        i(cls := "fa fa-copy"),
                        onClick --> Action
                          .eval(dispatch.send(UiEvent.BikeComponentCopyRequest(p)))
                          .pipe
                      ),
                      a(
                        cls := Css.iconLink,
                        i(cls := "fa fa-pencil"),
                        onClick --> Action
                          .eval(dispatch.send(UiEvent.BikeComponentEditRequest(p)))
                          .pipe
                      )
                    )
                  )
                }
              )
            )
        )
      )

  private def subscribe(model: SignallingRef[IO, TableModel], client: KeeperClient[IO])(
      dispatch: UiEventDispatch[IO]
  ) =
    dispatch.subscribe.evalMap {
      case UiEvent.BikeComponentCreated(_, _) =>
        doSearch(model, client)

      case UiEvent.BikeComponentUpdated(_, _) =>
        doSearch(model, client)

      case _ => IO.unit
    }

  def doSearch(model: SignallingRef[IO, TableModel], client: KeeperClient[IO]) =
    model
      .updateAndGet(TableModel.searchTrue)
      .map(_.query)
      .flatMap(client.searchComponents)
      .flatMap {
        case FetchResult.Success(res) => model.update(TableModel.components.replace(res))
        case FetchResult.RequestFailed(_) => model.update(TableModel.searchFalse)
      }

  def getBikes(model: SignallingRef[IO, TableModel], client: KeeperClient[IO]) =
    model.get.map(_.bikes.isEmpty).flatMap {
      case false => IO.unit
      case true =>
        IO(Instant.now).flatMap(client.getBikesAt).flatMap {
          case FetchResult.Success(res) => model.update(TableModel.bikes.replace(res))
          case FetchResult.RequestFailed(err) =>
            logger.error(s"Error getting current bikes: $err")
        }
    }
}
