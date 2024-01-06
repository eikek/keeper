package keeper.webview.client.newservice

import cats.effect.*
import cats.syntax.all.*
import fs2.concurrent.{Signal, SignallingRef}
import fs2.dom.HtmlDivElement

import keeper.bikes.data.{ComponentType, ComponentWithDevice}
import keeper.bikes.event.ServiceEvent.WaxChain.WaxType
import keeper.bikes.model.BikeBuilds
import keeper.common.Lenses
import keeper.common.Lenses.syntax.*
import keeper.webview.client.icons.ComponentIcon
import keeper.webview.client.shared._

import calico.html.io.{*, given}

object WaxChainForm {

  def render(
      model: SignallingRef[IO, WaxChainModel],
      components: Signal[IO, Map[ComponentType, List[ComponentWithDevice]]],
      builds: Signal[IO, BikeBuilds]
  ): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := Css.form + Css("pb-4") + Css.divideBorder,
      div(
        cls := Css.flexRowCenter + Css("mb-4 space-x-2"),
        ShowMountedBtn.render(model.to(WaxChainModel.showMounted))
      ),
      waxTypeSelect.render(
        model.to(WaxChainModel.waxType.andThen(Lenses.optionSet)),
        "waxtype-select",
        "Wax Type"
      ),
      div(
        cls := Css.of(
          Css.flexRowCenter,
          Css.inputLabel,
          Css("mt-3")
        ),
        ComponentIcon(ComponentType.Chain, cls := Css("h-6 w-6 mr-2")),
        "Select chains"
      ),
      builds.map(_.componentTotals).changes.map { totals =>
        ComponentSimpleSelect.render(
          model.to(WaxChainModel.chains),
          (
            model.map(_.showMounted),
            components.map(_.getOrElse(ComponentType.Chain, Nil))
          )
            .mapN { (showMounted, list) =>
              list.filter(e => showMounted || e.device.isEmpty)
            },
          totals
        )
      }
    )

  def waxTypeSelect =
    SimpleSelect.create[WaxType](
      WaxType.values.toList,
      _.name,
      str => WaxType.fromString(str).toOption
    )
}
