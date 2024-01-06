package keeper.webview.client.dashboard

import cats.effect.IO
import cats.effect.kernel.Resource
import cats.syntax.all.*
import fs2.concurrent.{Signal, SignallingRef}
import fs2.dom.HtmlDivElement

import keeper.bikes.model.{Bike, BikePart}
import keeper.common.Distance
import keeper.core.ComponentId
import keeper.webview.client.icons.{Bicycle, ComponentIcon}
import keeper.webview.client.shared.Css
import keeper.webview.client.util.Action

import calico.html.io.{*, given}

object BikeDiv {
  private val svgIconStyle = Css.strokeColor + Css.fillColor

  def apply(
      bike: Bike,
      details: SignallingRef[IO, Boolean],
      totals: Signal[IO, Option[Distance]],
      compTotals: Signal[IO, Map[ComponentId, Distance]],
      size: Size = Size.xl,
      styles: Css = Css.none
  ): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := Css.flexCol + styles,
      div(
        cls := Css.of(
          Css.flexRowCenter,
          Css("cursor-pointer"),
          size.title,
          Css("px-2"),
          Css.blueBoxed,
          Css.textColorViz
        ),
        onClick --> Action.all(
          Action.eval(details.update(flag => !flag))
        ),
        Bicycle(cls := size.bikeIcon + Css("mx-2"))(svgIconStyle),
        div(
          cls := "grow",
          span(cls := Css("mr-2 opacity-75"), bike.brand.name),
          span(bike.name)
        ),
        div(
          cls <-- totals.map(_.nonEmpty).changes.map(Css("italic ml-3 mr-1").showWhen),
          totals.map(dst => span(cls := "text-base", dst.map(_.show)))
        )
      ),
      div(
        cls <-- details.map(visible =>
          Css.of(
            Css.flexCol.showWhen(visible),
            Css("px-2 divide-y divide-dashed"),
            Css.divideBorder
          )
        ),
        bike.fork.map(f =>
          compDiv(
            f,
            size,
            compTotals,
            f.brakeCaliper.map(bc =>
              compDivL(bc, size, compTotals, bc.pad.map(compDivL(_, size, compTotals)))
            ),
            f.mudguard.map(compDivL(_, size, compTotals))
          )
        ),
        bike.stem.map(compDiv(_, size, compTotals)),
        bike.handlebar.map(compDiv(_, size, compTotals)),
        bike.frontWheel.map(fw =>
          compDiv(
            fw,
            size,
            compTotals,
            fw.tire.map(compDivL(_, size, compTotals)),
            fw.innerTube.map(compDivL(_, size, compTotals)),
            fw.brakeDisc.map(compDivL(_, size, compTotals))
          )
        ),
        bike.rearWheel.map(rw =>
          compDiv(
            rw,
            size,
            compTotals,
            rw.cassette.map(compDivL(_, size, compTotals)),
            rw.tire.map(compDivL(_, size, compTotals)),
            rw.innerTube.map(compDivL(_, size, compTotals)),
            rw.brakeDisc.map(compDivL(_, size, compTotals))
          )
        ),
        bike.chain.map(compDiv(_, size, compTotals)),
        bike.frontDerailleur.map(compDiv(_, size, compTotals)),
        bike.rearBrake.map(br =>
          compDivL(br, size, compTotals, br.pad.map(compDivL(_, size, compTotals)))
        ),
        bike.rearDerailleur.map(compDiv(_, size, compTotals)),
        bike.rearMudguard.map(compDiv(_, size, compTotals)),
        bike.seatpost.map(compDiv(_, size, compTotals)),
        bike.saddle.map(compDiv(_, size, compTotals))
      )
    )

  private def compDiv(
      c: BikePart,
      size: Size,
      totals: Signal[IO, Map[ComponentId, Distance]],
      children: Option[Resource[IO, HtmlDivElement[IO]]]*
  ): Resource[IO, HtmlDivElement[IO]] =
    compDiv1(c, Css("py-3"), size.titleIcon + Css("mx-2"), size, totals, children)

  private def compDivL(
      c: BikePart,
      size: Size,
      totals: Signal[IO, Map[ComponentId, Distance]],
      children: Option[Resource[IO, HtmlDivElement[IO]]]*
  ): Resource[IO, HtmlDivElement[IO]] =
    compDiv1(c, Css("pt-0.5"), size.lineIcon + Css("mx-2"), size, totals, children)

  private def compDiv1(
      c: BikePart,
      css: Css,
      iconCss: Css,
      size: Size,
      totals: Signal[IO, Map[ComponentId, Distance]],
      children: Seq[Option[Resource[IO, HtmlDivElement[IO]]]]
  ): Resource[IO, HtmlDivElement[IO]] =
    val ch = children.toList.flatten
    div(
      cls := Css.flexCol + css,
      div(
        cls := Css.flexRowCenter + Css(
          "dark:hover:bg-blue-500 dark:hover:bg-opacity-25 px-2 rounded py-1"
        ),
        title := s"${c.product.brand.name} ${c.product.product.name}",
        ComponentIcon(
          c.product.product.productType,
          cls := iconCss
        ),
        div(
          cls := size.line,
          span(cls := Css("font-semibold") + Css.textColorViz, c.name)
        ),
        totals
          .map(_.get(c.id))
          .changes
          .map(
            _.map(dst =>
              div(
                cls := Css("ml-2 text-sm grow text-right"),
                dst.show
              )
            )
          )
      ),
      div(
        cls := (Css.flexCol + Css("ml-4 pl-4")).showWhen(ch.nonEmpty),
        ch.sequence
      )
    )

  case class Size(bikeIcon: Css, titleIcon: Css, title: Css, lineIcon: Css, line: Css)
  object Size:
    val xl = Size(
      Css("h-16 w-16"),
      Css("w-10 h-10"),
      Css("text-2xl"),
      Css("w-8 h-8"),
      Css("text-lg")
    )
    val base = Size(
      Css("h-10 w-10"),
      Css("w-8 h-8"),
      Css("text-xl"),
      Css("w-6 h-6"),
      Css("text-base")
    )
    val sm =
      Size(Css("h-8 w-8"), Css("w-6 h-6"), Css("text-lg"), Css("w-4 h-4"), Css("text-sm"))
}
