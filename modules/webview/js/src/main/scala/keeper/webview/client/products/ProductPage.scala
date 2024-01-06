package keeper.webview.client.products

import java.time.ZoneId

import cats.effect.{IO, Resource}
import fs2.concurrent.SignallingRef
import fs2.dom.HtmlDivElement

import keeper.client.KeeperClient
import keeper.common.Lenses.syntax.*
import keeper.webview.client.cmd.*
import keeper.webview.client.shared.Css

import calico.html.io.{*, given}

object ProductPage {

  def render(
      model: SignallingRef[IO, Model],
      client: KeeperClient[IO],
      dispatch: UiEventDispatch[IO],
      zoneId: ZoneId
  ): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := Css("container mx-auto mt-4 px-2") + Css.flexCol,
      div(cls := Css.firstHeadline, "Products"),
      p(
        cls := Css("text-lg"),
        "This is a catalogue of products. Every component of a bike is associated to a specific product. ",
        "A product always has a brand associated. There is a list of brands available to choose from. If ",
        "yours is not included, just create it first."
      ),
      ProductForm.render(model.to(Model.form), client, dispatch),
      ProductTable.render(model.to(Model.table), client, dispatch, zoneId)
    )
}
