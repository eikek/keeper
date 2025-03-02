package keeper.webview.client.newservice

import cats.Eq
import cats.effect.{IO, Resource}
import cats.syntax.all.*
import fs2.concurrent.{Signal, SignallingRef}
import fs2.dom.HtmlDivElement

import keeper.bikes.data.{ComponentType, ComponentWithDevice}
import keeper.bikes.model.*
import keeper.common.Distance
import keeper.common.Lenses.syntax.*
import keeper.core.ComponentId
import keeper.webview.client.shared.{Css, ShowMountedBtn, SimpleSelect}
import keeper.webview.client.util.ComponentLabel

import calico.html.io.{*, given}

object ChangeWheelForm {
  private type Wheels[A] = (Map[ComponentId, Distance], List[A])

  def renderFrontWheel(
      model: SignallingRef[IO, ChangeWheelModel],
      builds: Signal[IO, BikeBuilds],
      components: Signal[IO, Map[ComponentType, List[ComponentWithDevice]]]
  ): Resource[IO, HtmlDivElement[IO]] =
    render1[FrontWheel](
      model,
      builds.map(m => (m.componentTotals, m.frontWheels)),
      components,
      List(
        ((w: FrontWheel) => w.tire) -> ComponentType.Tire,
        ((w: FrontWheel) => w.innerTube) -> ComponentType.InnerTube,
        ((w: FrontWheel) => w.brakeDisc) -> ComponentType.BrakeDisc
      )
    )

  def renderRearWheel(
      model: SignallingRef[IO, ChangeWheelModel],
      builds: Signal[IO, BikeBuilds],
      components: Signal[IO, Map[ComponentType, List[ComponentWithDevice]]]
  ): Resource[IO, HtmlDivElement[IO]] =
    render1[RearWheel](
      model,
      builds.map(m => (m.componentTotals, m.rearWheels)),
      components,
      List(
        ((w: RearWheel) => w.tire) -> ComponentType.Tire,
        ((w: RearWheel) => w.brakeDisc) -> ComponentType.BrakeDisc,
        ((w: RearWheel) => w.innerTube) -> ComponentType.InnerTube,
        ((w: RearWheel) => w.cassette) -> ComponentType.Cassette
      )
    )

  def render1[A <: BikePart: Eq](
      model: SignallingRef[IO, ChangeWheelModel],
      parts: Signal[IO, Wheels[A]],
      components: Signal[IO, Map[ComponentType, List[ComponentWithDevice]]],
      config: List[(A => Option[BikePart], ComponentType)]
  ): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := Css.form + Css("pb-4") + Css.divideBorder,
      div(
        cls := Css.secondHeadline,
        wheelSelect(parts.map(_._2))
          .render(model.to(ChangeWheelModel.wheel), "wheel-select", "")
      ),
      (parts, model).mapN((cs, m) => (cs._1, m.selectedPart(cs._2))).changes.map {
        case (_, None)             => div(cls := Css.hidden)
        case (totals, Some(wheel)) => wheelForm(wheel, config, model, components, totals)
      }
    )

  def wheelForm[A <: BikePart](
      wheel: A,
      config: List[(A => Option[BikePart], ComponentType)],
      model: SignallingRef[IO, ChangeWheelModel],
      components: Signal[IO, Map[ComponentType, List[ComponentWithDevice]]],
      totals: Map[ComponentId, Distance]
  ) =
    div(
      cls := Css.flexCol + Css("w-full"),
      div(
        cls := Css.flexRowCenter + Css("mb-2"),
        ShowMountedBtn.render(model.to(ChangeWheelModel.showMounted))
      ),
      div(
        cls := (Css.flexCol + Css("space-y-2 w-full")),
        config.map(c =>
          partDiv(
            c._1(wheel),
            c._2,
            components,
            model,
            totals
          )
        )
      )
    )

  def partDiv(
      current: Option[BikePart],
      ct: ComponentType,
      components: Signal[IO, Map[ComponentType, List[ComponentWithDevice]]],
      model: SignallingRef[IO, ChangeWheelModel],
      totals: Map[ComponentId, Distance]
  ) =
    BikePartOption.render(
      None,
      ct,
      Some(current),
      components,
      model.to(ChangeWheelModel.forType(ct)),
      model.map(_.showMounted),
      totals
    )

  def wheelSelect[A <: BikePart: Eq](model: Signal[IO, List[A]]) =
    SimpleSelect
      .create[A, ComponentId](
        model,
        b => b.id,
        b => ComponentLabel(b),
        str => str.toLongOption.map(ComponentId(_))
      )

}
