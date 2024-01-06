package keeper.webview.client.brands

import java.time.ZoneId

import cats.effect.{IO, Resource}
import fs2.concurrent.SignallingRef
import fs2.dom.HtmlDivElement

import keeper.client.KeeperClient
import keeper.common.Lenses.syntax.*
import keeper.webview.client.cmd.*
import keeper.webview.client.shared.Css

import calico.html.io.{*, given}

object BrandPage {

  def render(
      model: SignallingRef[IO, Model],
      client: KeeperClient[IO],
      dispatch: UiEventDispatch[IO],
      zoneId: ZoneId
  ): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := Css("container mx-auto mt-4 px-2") + Css.flexCol,
      div(cls := Css.firstHeadline, "Brands"),
      p(
        cls := Css("text-lg"),
        "Add more brands as needed."
      ),
      BrandForm.render(model.to(Model.form), client, dispatch, zoneId),
      BrandTable.render(model.to(Model.table), client, dispatch)
    )
}
