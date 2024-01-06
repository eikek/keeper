package keeper.webview.client.newservice

import java.time.ZoneId

import cats.effect.*
import fs2.concurrent.{Signal, SignallingRef}
import fs2.dom.HtmlDivElement

import keeper.bikes.data.{ComponentType, ComponentWithDevice}
import keeper.bikes.model.BikeBuilds
import keeper.common.Lenses.syntax.*
import keeper.webview.client.shared.{ComponentTableSelect, Css}

import calico.html.io.{*, given}

object CleanComponentForm {
  def render(
      model: SignallingRef[IO, CleanComponentModel],
      components: Signal[IO, Map[ComponentType, List[ComponentWithDevice]]],
      builds: Signal[IO, BikeBuilds],
      zoneId: ZoneId
  ): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := Css.flexCol,
      div(
        cls := Css.form,
        div(
          cls := Css.formField,
          label(cls := Css.inputLabel, "Choose components"),
          builds.map(_.componentTotals).changes.map { totals =>
            ComponentTableSelect.render(
              model.to(CleanComponentModel.select),
              components,
              totals,
              zoneId
            )
          }
        )
      )
    )
}
