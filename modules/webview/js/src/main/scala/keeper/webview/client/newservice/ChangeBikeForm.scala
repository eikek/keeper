package keeper.webview.client.newservice

import cats.effect.*
import cats.syntax.all.*
import fs2.concurrent.{Signal, SignallingRef}
import fs2.dom.HtmlDivElement

import keeper.bikes.data.{ComponentType, ComponentWithDevice}
import keeper.bikes.model.{Bike, BikeBuilds, BikePart}
import keeper.common.Distance
import keeper.common.Lenses.syntax.*
import keeper.core.{ComponentId, DeviceId}
import keeper.webview.client.shared.{Css, ShowMountedBtn, SimpleSelect}
import keeper.webview.client.util.BikeLabel

import calico.html.io.{*, given}

object ChangeBikeForm {

  def render(
      model: SignallingRef[IO, ChangeBikeModel],
      bikes: Signal[IO, BikeBuilds],
      components: Signal[IO, Map[ComponentType, List[ComponentWithDevice]]]
  ): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := Css.form + Css("pb-4") + Css.divideBorder,
      div(
        cls := Css.secondHeadline,
        bikeSelect(bikes.map(_.bikes))
          .render(model.to(ChangeBikeModel.bike), "bike-select", "")
      ),
      (bikes, model)
        .mapN((bs, m) => (bs.componentTotals, m.selectedBike(bs.bikes)))
        .changes
        .map {
          case (_, None)            => div(cls := Css.hidden)
          case (totals, Some(bike)) => bikeForm(model, components, totals, bike)
        }
    )

  def bikeForm(
      model: SignallingRef[IO, ChangeBikeModel],
      components: Signal[IO, Map[ComponentType, List[ComponentWithDevice]]],
      totals: Map[ComponentId, Distance],
      bike: Bike
  ): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := Css.flexCol + Css("w-full"),
      div(
        cls := Css.flexRowCenter + Css("mb-2"),
        ShowMountedBtn.render(model.to(ChangeBikeModel.showMounted))
      ),
      div(
        cls := Css.flexCol + Css("space-y-2 w-full"),
        partDiv(
          bike,
          ComponentType.Chain,
          bike.chain,
          components,
          totals,
          model
        ),
        partDiv(
          bike,
          ComponentType.FrontWheel,
          bike.frontWheel,
          components,
          totals,
          model
        ),
        partDiv(
          bike,
          ComponentType.RearWheel,
          bike.rearWheel,
          components,
          totals,
          model
        ),
        partDiv(
          bike,
          ComponentType.Handlebar,
          bike.handlebar,
          components,
          totals,
          model
        ),
        partDiv(
          bike,
          ComponentType.Stem,
          bike.stem,
          components,
          totals,
          model
        ),
        partDiv(
          bike,
          ComponentType.CrankSet,
          bike.crankSet,
          components,
          totals,
          model
        ),
        partDiv(
          bike,
          ComponentType.Fork,
          bike.fork,
          components,
          totals,
          model
        ),
        partDiv(
          bike,
          ComponentType.FrontDerailleur,
          bike.frontDerailleur,
          components,
          totals,
          model
        ),
        partDiv(
          bike,
          ComponentType.RearDerailleur,
          bike.rearDerailleur,
          components,
          totals,
          model
        ),
        partDiv(
          bike,
          ComponentType.Seatpost,
          bike.seatpost,
          components,
          totals,
          model
        ),
        partDiv(
          bike,
          ComponentType.Saddle,
          bike.saddle,
          components,
          totals,
          model
        ),
        partDiv(
          bike,
          ComponentType.RearBrake,
          bike.rearBrake,
          components,
          totals,
          model
        ),
        partDiv(
          bike,
          ComponentType.RearMudguard,
          bike.rearMudguard,
          components,
          totals,
          model
        )
      )
    )

  private def partDiv(
      bike: Bike,
      ct: ComponentType,
      current: Option[BikePart],
      components: Signal[IO, Map[ComponentType, List[ComponentWithDevice]]],
      totals: Map[ComponentId, Distance],
      model: SignallingRef[IO, ChangeBikeModel]
  ) =
    BikePartOption.render(
      bike.id.some,
      ct,
      Some(current),
      components,
      model.to(ChangeBikeModel.forType(ct)),
      model.map(_.showMounted),
      totals
    )

  def bikeSelect(model: Signal[IO, List[Bike]]) =
    SimpleSelect
      .create[Bike, DeviceId](
        model,
        b => b.id,
        b => BikeLabel(b),
        str => str.toLongOption.map(DeviceId(_))
      )
}
