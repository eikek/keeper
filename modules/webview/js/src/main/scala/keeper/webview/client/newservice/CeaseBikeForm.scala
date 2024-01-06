package keeper.webview.client.newservice

import cats.effect.*
import cats.syntax.all.*
import fs2.concurrent.{Signal, SignallingRef}
import fs2.dom.HtmlDivElement

import keeper.bikes.model.Bike
import keeper.common.Lenses.syntax.*
import keeper.core.DeviceId
import keeper.webview.client.shared.{Checkbox, Css, SimpleSelect}
import keeper.webview.client.util.BikeLabel

import calico.html.io.{*, given}

object CeaseBikeForm {

  def render(
      model: SignallingRef[IO, CeaseBikeModel],
      bikes: Signal[IO, List[Bike]]
  ): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := Css.form + Css("pb-4") + Css.divideBorder,
      div(
        cls := Css.secondHeadline,
        bikeSelect(bikes).render(model.to(CeaseBikeModel.bike), "bike-select", "")
      ),
      (bikes, model).mapN((bs, m) => m.selectedBike(bs)).changes.map {
        case None       => div(cls := Css.hidden)
        case Some(bike) => bikeForm(model, bike)
      }
    )

  def bikeForm(model: SignallingRef[IO, CeaseBikeModel], bike: Bike) =
    div(
      cls := Css.form,
      div(
        cls := Css.firstFormField,
        label(cls := Css.inputLabel, "Include all components"),
        Checkbox.render(
          model.to(CeaseBikeModel.withComps),
          Some("Include all components!")
        )
      )
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
