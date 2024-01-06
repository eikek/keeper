package keeper.webview.client.newservice

import cats.effect.*
import cats.syntax.all.*
import fs2.concurrent.{Signal, SignallingRef}
import fs2.dom.HtmlDivElement

import keeper.bikes.data.{ComponentType, ComponentWithDevice}
import keeper.bikes.model.{Bike, BikeBuilds}
import keeper.common.Distance
import keeper.common.Lenses.syntax.*
import keeper.core.ComponentId
import keeper.webview.client.shared.{Css, ShowMountedBtn}

import calico.html.io.{*, given}

object ChangeTiresForm {

  def render(
      model: SignallingRef[IO, ChangeTiresModel],
      builds: Signal[IO, BikeBuilds],
      components: Signal[IO, Map[ComponentType, List[ComponentWithDevice]]]
  ): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := Css.form + Css("pb-4") + Css.divideBorder,
      div(
        cls := Css.secondHeadline,
        ChangeBikeForm
          .bikeSelect(builds.map(_.bikes))
          .render(model.to(ChangeTiresModel.bike), "bike-select", "")
      ),
      (builds, model)
        .mapN((bs, m) => (bs.componentTotals, m.selectedBike(bs.bikes)))
        .changes
        .map {
          case (_, None)            => div(cls := Css.hidden)
          case (totals, Some(bike)) => bikeForm(model, components, totals, bike)
        }
    )

  def bikeForm(
      model: SignallingRef[IO, ChangeTiresModel],
      components: Signal[IO, Map[ComponentType, List[ComponentWithDevice]]],
      totals: Map[ComponentId, Distance],
      bike: Bike
  ): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := Css.form,
      div(
        cls := Css.flexRowCenter + Css("mb-2"),
        ShowMountedBtn.render(model.to(ChangeTiresModel.showMounted))
      ),
      div(
        cls := Css.flexCol,
        div(
          cls := Css.secondHeadline,
          "Front"
        ),
        BikePartOption.render(
          bike.id.some,
          ComponentType.Tire,
          bike.frontWheel.map(_.tire),
          components,
          model.to(ChangeTiresModel.front),
          model.map(_.showMounted),
          totals
        )
      ),
      div(
        cls := Css.flexCol,
        div(
          cls := Css.secondHeadline,
          "Rear"
        ),
        BikePartOption.render(
          bike.id.some,
          ComponentType.Tire,
          bike.rearWheel.map(_.tire),
          components,
          model.to(ChangeTiresModel.rear),
          model.map(_.showMounted),
          totals
        )
      )
    )
}
