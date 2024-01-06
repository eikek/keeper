package keeper.webview.client.newservice

import cats.effect.*
import fs2.concurrent.{Signal, SignallingRef}
import fs2.dom.HtmlDivElement

import keeper.bikes.model.Bike
import keeper.common.Lenses.syntax.*
import keeper.webview.client.shared.{BikeMultiSelect, Css}

import calico.html.io.{*, given}

object CleanBikeForm {
  def render(
      model: SignallingRef[IO, CleanBikeModel],
      bikes: Signal[IO, List[Bike]]
  ): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := Css.form + Css("pb-4") + Css.divideBorder,
      div(
        cls := Css.formField,
        label(cls := Css.inputLabel, "Choose bikes"),
        BikeMultiSelect.render(
          model.to(CleanBikeModel.bikes),
          bikes
        )
      )
    )
}
