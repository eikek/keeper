package keeper.webview.client.products

import java.time.ZoneId

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

object ProductTable {

  def render(
      model: SignallingRef[IO, TableModel],
      client: KeeperClient[IO],
      dispatch: UiEventDispatch[IO],
      zoneId: ZoneId
  ): Resource[IO, HtmlDivElement[IO]] =
    subscribe(model, client)(dispatch).compile.drain.background >>
      Resource.eval(doSearch(model, client)) >>
      div(
        cls := Css.flexCol + Css("mt-8 w-full"),
        div(
          cls := Css.flexRowCenter + Css("mb-2"),
          input.withSelf { self =>
            (
              cls := Css.textInput,
              placeholder := "Search products…",
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
              th(cls := Css.tableHeadCellMd + Css("w-2/12"), "Type"),
              th(cls := Css.tableHeadCell + Css("w-6/12"), "Name"),
              th(cls := Css.tableHeadCellMd + Css("w-2/12"), "Added"),
              th(cls := Css.tableHeadCell + Css("w-6/12 md:w-2/12"), "")
            )
          ),
          model
            .map(_.products)
            .changes
            .map(products =>
              tbody(
                products.map(p =>
                  tr(
                    cls := Css.tableRow,
                    td(
                      cls := Css.tableRowCellMd,
                      div(
                        cls := Css.flexRowCenter,
                        ComponentIcon(
                          p.product.productType,
                          cls := "h-8 w-8"
                        ),
                        div(
                          cls := "align-middle ml-2",
                          p.product.productType.name
                        )
                      )
                    ),
                    td(
                      cls := Css.tableRowCell,
                      s"${p.brand.name} ${p.product.name}"
                    ),
                    td(
                      cls := Css.tableRowCellMd,
                      FormatDate(p.product.createdAt, zoneId)
                    ),
                    td(
                      cls := Css.tableRowCell + Css.flexRowCenter + Css("justify-end"),
                      a(
                        cls := Css.iconLink + Css("mr-2"),
                        i(cls := "fa fa-copy"),
                        onClick --> Action
                          .eval(dispatch.send(UiEvent.BikeProductCopyRequest(p)))
                          .pipe
                      ),
                      a(
                        cls := Css.iconLink,
                        i(cls := "fa fa-pencil"),
                        onClick --> Action
                          .eval(dispatch.send(UiEvent.BikeProductEditRequest(p)))
                          .pipe
                      )
                    )
                  )
                )
              )
            )
        )
      )

  private def subscribe(model: SignallingRef[IO, TableModel], client: KeeperClient[IO])(
      dispatch: UiEventDispatch[IO]
  ) =
    dispatch.subscribe.evalMap {
      case UiEvent.BikeProductCreated(_, _) =>
        doSearch(model, client)

      case UiEvent.BikeProductUpdated(_, _) =>
        doSearch(model, client)

      case _ => IO.unit
    }

  private def doSearch(model: SignallingRef[IO, TableModel], client: KeeperClient[IO]) =
    model
      .updateAndGet(TableModel.searchTrue)
      .map(_.query)
      .flatMap(client.searchProducts)
      .flatMap {
        case FetchResult.Success(res) => model.update(TableModel.products.replace(res))
        case FetchResult.RequestFailed(_) => model.update(TableModel.searchFalse)
      }

}
