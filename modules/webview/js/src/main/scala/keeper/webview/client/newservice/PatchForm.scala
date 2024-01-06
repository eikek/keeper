package keeper.webview.client.newservice
import cats.effect.*
import cats.syntax.all.*
import fs2.concurrent.{Signal, SignallingRef}
import fs2.dom.HtmlDivElement

import keeper.bikes.data.{ComponentType, ComponentWithDevice}
import keeper.bikes.model.BikeBuilds
import keeper.common.Lenses.syntax.*
import keeper.common.{Distance, Lenses}
import keeper.webview.client.icons.ComponentIcon
import keeper.webview.client.shared.{ComponentSimpleSelect, Css, ShowMountedBtn}

import calico.html.io.{*, given}

object PatchForm {
  def render(
      model: SignallingRef[IO, PatchModel],
      componentType: ComponentType,
      components: Signal[IO, Map[ComponentType, List[ComponentWithDevice]]],
      builds: Signal[IO, BikeBuilds]
  ): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := Css.form + Css("pb-4") + Css.divideBorder,
      div(
        cls := Css.flexRowCenter + Css("mb-4 space-x-2"),
        ShowMountedBtn.render(model.to(PatchModel.showMounted))
      ),
      div(
        cls := Css.of(
          Css.flexRowCenter,
          Css.inputLabel,
          Css("mt-3")
        ),
        ComponentIcon(componentType, cls := Css("h-6 w-6 mr-2")),
        s"Select ${componentType.name}s"
      ),
      builds.map(_.componentTotals).changes.map { totals =>
        ComponentSimpleSelect.render(
          model.to(PatchModel.components),
          (model.map(_.showMounted), components.map(_.getOrElse(componentType, Nil)))
            .mapN { (showMounted, list) =>
              list.filter(e => showMounted || e.device.isEmpty)
            },
          totals
        )
      }
    )
}
