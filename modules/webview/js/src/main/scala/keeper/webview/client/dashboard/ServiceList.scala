package keeper.webview.client.dashboard

import java.time.ZoneId

import cats.effect.{IO, Resource}
import cats.kernel.Eq
import cats.syntax.all.*
import fs2.concurrent.{Signal, SignallingRef}
import fs2.dom.{HtmlAnchorElement, HtmlDivElement}

import keeper.bikes.data.ActionName
import keeper.bikes.model.ServiceDetail
import keeper.bikes.model.ServiceDetail.{BikeAndName, ComponentInfo}
import keeper.webview.client.dashboard.ServiceEntryIter.{Action, Diff, ParentComp}
import keeper.webview.client.icons.{Bicycle, ComponentIcon}
import keeper.webview.client.shared.Css
import keeper.webview.client.util.{Action as On, FormatDate}

import calico.html.Modifier
import calico.html.io.{*, given}

object ServiceList {

  def render(
      data: Signal[IO, List[ServiceDetail]],
      searchMask: SignallingRef[IO, SearchMaskModel],
      zoneId: ZoneId
  ): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := Css("text-base px-2"),
      data.changes
        .map(list =>
          div(
            cls := Css.flexCol + Css("divide-y divide-dashed") + Css.divideBorder,
            list.map(s => serviceEntry(searchMask, list, s, zoneId))
          )
        )
    )

  def serviceEntry(
      search: SignallingRef[IO, SearchMaskModel],
      all: List[ServiceDetail],
      s: ServiceDetail,
      zoneId: ZoneId
  ): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := Css.flexCol + Css(
        "px-2 py-2 dark:hover:bg-slate-600/20"
      ),
      idAttr := s"service-${s.id}",
      div(
        cls := Css.flexRowMd + Css("font-semibold") + Css.textColorViz,
        div(
          cls := Css.flexRowCenter + Css("grow"),
          div(
            cls := Css("pr-2 mr-4"),
            FormatDate(s.date, zoneId)
          ),
          div(
            cls := "grow",
            s.name
          )
        ),
        div(
          cls := Css.flexRowCenter + Css("space-x-1 my-2 lg:my-0"),
          s.affectedBikes.toList.map(b =>
            a(
              cls := Css.label,
              href := "#",
              onClick --> On
                .eval(
                  search.update(
                    SearchMaskModel.searchBikesAt(b.toBikeAndName).replace(true)
                  )
                )
                .pipe,
              span(b.name),
              span(
                cls := Css("ml-1 text-sm").showWhen(b.total.isDefined),
                "@",
                b.total.map(_.show)
              )
            )
          )
        )
      ),
      div(
        cls := Css("py-2 px-6 text-wrap max-w-screen-lg text-justify text-sm")
          .showWhen(
            s.description.isDefined
          ),
        s.description
      ),
      div(
        cls := Css.flexCol + Css("ml-4"),
        ServiceEntryIter(s, ServiceEntryIter.FindPrevious.search(all))
          .traverse(renderElement(search)(Css.none, s, zoneId, _))
      )
    )

  def renderElement(search: SignallingRef[IO, SearchMaskModel])(
      css: Css,
      s: ServiceDetail,
      zoneId: ZoneId,
      element: ServiceEntryIter.Element
  ): Resource[IO, HtmlDivElement[IO]] =
    element match
      case ServiceEntryIter.Bike(bike, ceased, children) =>
        div(
          cls := Css.flexCol + css,
          div(
            cls := Css.of(
              Css.flexRowCenter,
              (Css.textColorViz + Css("font-lg")).when(!ceased),
              Css("text-red-400 font-semibold font-lg").when(ceased)
            ),
            Bicycle(cls := "h-6 w-6 mr-2")(
              (Css.fillColor + Css.strokeColor).when(!ceased) +
                Css("fill-red-400 stroke-red-400").when(ceased)
            ),
            bike.name,
            Option.when(ceased)(
              div(cls := "ml-2", div(cls := "fa fa-skull-crossbones"))
            )
          ),
          children.toList.traverse(e =>
            renderChildElement(search, zoneId)(Css("ml-3"), s, e)
          )
        )
      case e @ ServiceEntryIter.ParentComp(comp, diffs) =>
        renderChildElement(search, zoneId)(Css(""), s, e)
      case e: ServiceEntryIter.Action =>
        renderAction(search, zoneId)(Css(""), s, e)

  private def renderChildElement(
      search: SignallingRef[IO, SearchMaskModel],
      zoneId: ZoneId
  )(
      css: Css,
      s: ServiceDetail,
      el: ParentComp | Diff | Action
  ): Resource[IO, HtmlDivElement[IO]] =
    el match
      case e: ParentComp => renderParentComp(search)(css, s, e)
      case e: Diff       => renderDiff(search)(s, e, css)
      case e: Action     => renderAction(search, zoneId)(css, s, e)

  private def renderAction(
      search: SignallingRef[IO, SearchMaskModel],
      zoneId: ZoneId
  )(css: Css, s: ServiceDetail, act: Action) =
    div(
      cls := Css.flexRowCenter + Css("py-2 md:py-0.5") + css,
      a(
        cls := Css.textColorViz + Css("mr-3"),
        href := "#",
        onClick --> On
          .eval(
            search.update(SearchMaskModel.searchActionAt(act.name).replace(true))
          )
          .pipe,
        actionNameDiv(act.name)
      ),
      act.component match {
        case i: ComponentInfo =>
          div(
            cls := Css.flexRowMd,
            renderComponentInfo(search)(s, i, componentIcon(i)),
            act.previous.map(prevService =>
              a(
                cls := Css("text-sm ml-2 opacity-70"),
                href := s"#service-${prevService.id}",
                title := s"Last ${act.name}",
                span(cls := "fa fa-clock-rotate-left mr-1"),
                FormatDate(prevService.date, zoneId),
                (prevService.totalsFor(i.id), s.totalsFor(i.id)).mapN((past, cur) =>
                  val diff = cur - past
                  span(cls := Css("ml-0.5 text-green-500"), show"+$diff")
                )
              )
            )
          )
        case b: BikeAndName =>
          div(
            cls := Css.flexRowCenter,
            Bicycle(cls := "h-6 w-6 mr-2")(),
            b.name
          )
      }
    )

  private def actionNameDiv(an: ActionName) =
    div(
      cls := (an match
        case ActionName.Cease =>
          Css("text-red-400 font-semibold")
        case _ =>
          Css.none),
      an match
        case ActionName.Cease =>
          span(cls := "fa fa-skull-crossbones mr-1")
        case _ =>
          span(cls := "hidden")
      ,
      an.asString
    )

  private def renderParentComp(
      search: SignallingRef[IO, SearchMaskModel]
  )(css: Css, s: ServiceDetail, e: ParentComp) =
    div(
      cls := Css.flexCol + css,
      renderComponentInfo(search)(
        s,
        e.comp,
        componentIcon(e.comp)
      ),
      div(
        cls := Css.flexCol + Css("ml-3"),
        e.diffs.toList.traverse(diff => renderDiff(search)(s, diff))
      )
    )

  private def renderComponentInfo[M](search: SignallingRef[IO, SearchMaskModel])(
      s: ServiceDetail,
      i: ServiceDetail.ComponentInfo,
      modifierBefore: M,
      divCss: Css = Css.none
  )(using
      M: Modifier[IO, HtmlAnchorElement[IO], M]
  ): Resource[IO, HtmlAnchorElement[IO]] =
    a(
      cls := Css.flexRowCenter + divCss,
      href := "#",
      onClick --> On
        .eval(
          search.update(SearchMaskModel.searchCompsAt(i).replace(true))
        )
        .pipe,
      modifierBefore,
      i.name,
      div(
        cls := Css("ml-1")
          .showWhen(s.totalsFor(i.id).exists(_.toKm > 0)),
        span(cls := "fa fa-at mr-1"),
        s.totalsFor(i.id).map(_.show)
      )
    )

  private def componentIcon(i: ComponentInfo) =
    ComponentIcon(i.typ, cls := Css("h-4 w-4 mr-1"))

  private def renderDiff[M](search: SignallingRef[IO, SearchMaskModel])(
      s: ServiceDetail,
      diff: ServiceEntryIter.Diff,
      divCss: Css = Css.none
  ): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := Css.flexRowMd + divCss,
      div(
        cls := Css.flexRowCenter,
        ComponentIcon(diff.typ, cls := Css("h-4 w-4 mr-2")),
        diff.typ.show
      ),
      div(
        cls := (Css.flexRowCenter + Css("ml-2 md:ml-0")).showWhen(diff.ceased.nonEmpty),
        diff.ceased.map(i =>
          renderComponentInfo(search)(
            s,
            i,
            div(
              cls := Css.flexRowCenter + Css("font-semibold opacity-75"),
              div(cls := "fa fa-skull-crossbones mr-1"),
              div(cls := Css("mr-2"), "cease")
            ),
            Css("mx-1 text-red-400")
          )
        )
      ),
      div(
        cls := (Css.flexRowCenter + Css("ml-2 md:ml-0")).showWhen(diff.removed.nonEmpty),
        diff.removed.map(i =>
          renderComponentInfo(search)(
            s,
            i,
            span(cls := "fa fa-minus"),
            Css("mx-1 text-red-500 opacity-75")
          )
        )
      ),
      div(
        cls := (Css.flexRowCenter + Css("ml-2 md:ml-0")).showWhen(diff.added.nonEmpty),
        diff.added.map(i =>
          renderComponentInfo(search)(
            s,
            i,
            span(cls := "fa fa-plus"),
            Css("mx-1 text-green-500")
          )
        )
      )
    )
}
