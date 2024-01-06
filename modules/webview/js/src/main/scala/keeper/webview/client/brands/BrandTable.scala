package keeper.webview.client.brands

import cats.effect.{IO, Resource}
import cats.syntax.all.*
import fs2.concurrent.SignallingRef
import fs2.dom.HtmlDivElement

import keeper.bikes.*
import keeper.client.KeeperClient
import keeper.client.data.FetchResult
import keeper.webview.client.cmd.*
import keeper.webview.client.shared.Css
import keeper.webview.client.util.Action

import calico.html.io.{*, given}
import org.scalajs.dom.KeyValue

object BrandTable {

  def render(
      model: SignallingRef[IO, TableModel],
      client: KeeperClient[IO],
      dispatch: UiEventDispatch[IO]
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
              placeholder := "Search brands…",
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
              th(cls := Css.tableHeadCell + Css("w-8/12"), "Name"),
              th(cls := Css.tableHeadCell + Css("w-4/12"), "")
            )
          ),
          model
            .map(_.brands)
            .changes
            .map(cs =>
              tbody(
                cs.map(brand =>
                  tr(
                    cls := Css.tableRow,
                    td(
                      cls := Css.tableRowCell,
                      brand.name
                    ),
                    td(
                      cls := Css.tableRowCell + Css.flexRowCenter + Css("justify-end"),
                      a(
                        cls := Css.iconLink,
                        i(cls := "fa fa-pencil"),
                        onClick --> Action
                          .eval(dispatch.send(UiEvent.BikeBrandEditRequest(brand)))
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
      case UiEvent.BikeBrandCreated(_, _) =>
        doSearch(model, client)

      case UiEvent.BikeBrandUpdated(_, _) =>
        doSearch(model, client)

      case _ => IO.unit
    }

  private def doSearch(model: SignallingRef[IO, TableModel], client: KeeperClient[IO]) =
    model
      .updateAndGet(TableModel.searchTrue)
      .map(_.query)
      .map(_.text.some.filter(_.nonEmpty))
      .flatMap(client.searchBrands)
      .flatMap {
        case FetchResult.Success(res)     => model.update(TableModel.brands.replace(res))
        case FetchResult.RequestFailed(_) => model.update(TableModel.searchFalse)
      }
}
