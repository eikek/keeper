package keeper.webview.client.newservice

import cats.Eq
import cats.effect.{IO, Resource}
import cats.syntax.all.*
import fs2.concurrent.{Signal, SignallingRef}
import fs2.dom.HtmlDivElement

import keeper.bikes.data.{ComponentType, ComponentWithDevice}
import keeper.bikes.model.{BikeBuilds, BikePart, Fork}
import keeper.common.Distance
import keeper.common.Lenses.syntax.*
import keeper.core.ComponentId
import keeper.webview.client.shared.{Css, ShowMountedBtn, SimpleSelect}
import keeper.webview.client.util.ComponentLabel

import calico.html.io.{*, given}

object ChangeForkForm {
  private type Forks = (Map[ComponentId, Distance], List[Fork])

  def render(
      model: SignallingRef[IO, ChangeForkModel],
      builds: Signal[IO, BikeBuilds],
      components: Signal[IO, Map[ComponentType, List[ComponentWithDevice]]]
  ): Resource[IO, HtmlDivElement[IO]] =
    render1(
      model,
      builds.map(m => (m.componentTotals, m.forks)),
      components,
      List(
        ((w: Fork) => w.brakeCaliper) -> ComponentType.FrontBrake,
        ((w: Fork) => w.mudguard) -> ComponentType.FrontMudguard
      )
    )

  def render1(
      model: SignallingRef[IO, ChangeForkModel],
      parts: Signal[IO, Forks],
      components: Signal[IO, Map[ComponentType, List[ComponentWithDevice]]],
      config: List[(Fork => Option[BikePart], ComponentType)]
  ): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := Css.form + Css("pb-4") + Css.divideBorder,
      div(
        cls := Css.secondHeadline,
        forkSelect(parts.map(_._2))
          .render(model.to(ChangeForkModel.fork), "fork-select", "")
      ),
      (parts, model).mapN((cs, m) => (cs._1, m.selectedPart(cs._2))).changes.map {
        case (_, None)            => div(cls := Css.hidden)
        case (totals, Some(fork)) => forkForm(fork, config, model, components, totals)
      }
    )

  def forkForm[A <: BikePart](
      wheel: A,
      config: List[(A => Option[BikePart], ComponentType)],
      model: SignallingRef[IO, ChangeForkModel],
      components: Signal[IO, Map[ComponentType, List[ComponentWithDevice]]],
      totals: Map[ComponentId, Distance]
  ) =
    div(
      cls := Css.flexCol + Css("w-full"),
      div(
        cls := Css.flexRowCenter + Css("mb-2"),
        ShowMountedBtn.render(model.to(ChangeForkModel.showMounted))
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
      model: SignallingRef[IO, ChangeForkModel],
      totals: Map[ComponentId, Distance]
  ) =
    BikePartOption.render(
      None,
      ct,
      Some(current),
      components,
      model.to(ChangeForkModel.forType(ct)),
      model.map(_.showMounted),
      totals
    )

  def forkSelect[A <: BikePart: Eq](model: Signal[IO, List[A]]) =
    SimpleSelect
      .create[A, ComponentId](
        model,
        b => b.id,
        b => ComponentLabel(b),
        str => str.toLongOption.map(ComponentId(_))
      )

}
