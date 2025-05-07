package keeper.webview.client.icons

import cats.effect.{IO, Resource}
import fs2.dom.{HtmlDivElement, HtmlElement}

import keeper.bikes.data.ComponentType
import keeper.webview.client.shared.Css

import calico.html.Modifier
import calico.html.io.{title, given}

object ComponentIcon {

  def apply[M](
      ct: ComponentType,
      containerMod: M,
      svgCss: Css = Css.fillColor + Css.strokeColor
  )(using
      M: Modifier[IO, HtmlDivElement[IO], M]
  ): Resource[IO, HtmlElement[IO]] =
    ct match
      case ComponentType.BrakeDisc =>
        BrakeDisc(containerMod, title := ct.name)(svgCss)
      case ComponentType.Handlebar =>
        Handlebar(containerMod, title := ct.name)(svgCss)
      case ComponentType.Tire =>
        Tire(containerMod, title := ct.name)(svgCss)
      case ComponentType.RearDerailleur =>
        RearDerailleur(containerMod, title := ct.name)(svgCss)
      case ComponentType.Stem =>
        Stem(containerMod, title := ct.name)(svgCss)
      case ComponentType.FrontWheel =>
        Wheel(containerMod, title := ct.name)(svgCss)
      case ComponentType.FrontMudguard =>
        FrontMudguard(containerMod, title := ct.name)(svgCss)
      case ComponentType.Fork =>
        Fork(containerMod, title := ct.name)(svgCss)
      case ComponentType.RearWheel =>
        Wheel(containerMod, title := ct.name)(svgCss)
      case ComponentType.Cassette =>
        Cassette(containerMod, title := ct.name)(svgCss)
      case ComponentType.Seatpost =>
        Seatpost(containerMod, title := ct.name)(svgCss)
      case ComponentType.FrontBrake =>
        RimBrake(containerMod, title := ct.name)(svgCss)
      case ComponentType.RearBrake =>
        RimBrake(containerMod, title := ct.name)(svgCss)
      case ComponentType.Chain =>
        Chain(containerMod, title := ct.name)(svgCss)
      case ComponentType.BrakePad =>
        BrakePad(containerMod, title := ct.name)(svgCss)
      case ComponentType.RearMudguard =>
        RearMudguard(containerMod, title := ct.name)(svgCss)
      case ComponentType.FrontDerailleur =>
        FrontDerailleur(containerMod, title := ct.name)(svgCss)
      case ComponentType.Saddle =>
        Saddle(containerMod, title := ct.name)(svgCss)
      case ComponentType.InnerTube =>
        InnerTube(containerMod, title := ct.name)(svgCss)
      case ComponentType.CrankSet =>
        CrankSet(containerMod, title := ct.name)(svgCss)
}
