package keeper.webview.client.components

import java.time.ZoneId

import cats.effect.{IO, Resource}
import fs2.concurrent.SignallingRef
import fs2.dom.HtmlDivElement

import keeper.client.KeeperClient
import keeper.common.Lenses.syntax.*
import keeper.webview.client.cmd.*
import keeper.webview.client.shared.Css

import calico.html.io.{*, given}

object ComponentPage {

  def render(
      model: SignallingRef[IO, Model],
      client: KeeperClient[IO],
      dispatch: UiEventDispatch[IO],
      zoneId: ZoneId
  ): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := Css("container mx-auto mt-4 px-2") + Css.flexCol,
      div(cls := Css.firstHeadline, "Components"),
      p(
        cls := Css("text-lg"),
        "This is your component inventory. Add every component you own that you wish to configure later to some bike."
      ),
      ComponentForm.render(model.to(Model.form), client, dispatch, zoneId),
      ComponentTable.render(model.to(Model.table), client, dispatch, zoneId)
    )
}
