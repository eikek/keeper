package keeper.webview.client.newservice

import java.time.ZoneId

import cats.effect.*
import fs2.concurrent.{Signal, SignallingRef}
import fs2.dom.HtmlDivElement

import keeper.bikes.data.{ComponentType, ComponentWithDevice}
import keeper.bikes.model.BikeBuilds
import keeper.common.Lenses.syntax.*
import keeper.webview.client.shared.{Checkbox, ComponentTableSelect, Css}

import calico.html.io.{*, given}

object CeaseComponentForm {

  def render(
      model: SignallingRef[IO, CeaseComponentModel],
      components: Signal[IO, Map[ComponentType, List[ComponentWithDevice]]],
      builds: Signal[IO, BikeBuilds],
      zoneId: ZoneId
  ): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := Css.flexCol,
      div(
        cls := Css.form,
        div(
          cls := Css.firstFormField,
          label(cls := Css.inputLabel, "With Sub-components?"),
          Checkbox.render(
            model.to(CeaseComponentModel.withSubs),
            Some("Include all sub components")
          )
        ),
        div(
          cls := Css.formField,
          label(cls := Css.inputLabel, "Choose components"),
          builds.map(_.componentTotals).changes.map { totals =>
            ComponentTableSelect
              .render(model.to(CeaseComponentModel.select), components, totals, zoneId)
          }
        )
      )
    )
}
