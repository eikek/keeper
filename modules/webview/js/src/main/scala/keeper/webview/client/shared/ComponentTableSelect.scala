package keeper.webview.client.shared

import java.time.ZoneId

import scala.collection.immutable.SortedSet

import cats.data.NonEmptySet
import cats.effect.{IO, Resource}
import cats.syntax.all.*
import fs2.concurrent.{Signal, SignallingRef}
import fs2.dom.HtmlDivElement

import keeper.bikes.data.{ComponentType, ComponentWithDevice}
import keeper.common.{Distance, Lenses}
import keeper.core.ComponentId
import keeper.webview.client.icons.ComponentIcon
import keeper.webview.client.util.{Action, FormatDate}

import calico.html.io.{*, given}
import monocle.{Lens, Monocle}
import org.scalajs.dom.KeyValue

object ComponentTableSelect {

  // -- Model
  final case class Model(
      components: Set[ComponentId] = Set.empty,
      filter: Option[String] = None
  ) {
    def applyFilter(
        cs: Map[ComponentType, List[ComponentWithDevice]]
    ): List[ComponentWithDevice] =
      filter
        .map(f => cs.values.flatten.filter(c => c.containsName(f)))
        .getOrElse(cs.values.flatten)
        .toList
        .sortBy(c => c.component.product.productType.name + c.component.component.name)

    def selectedComponents =
      NonEmptySet.fromSet(SortedSet.from(components))
  }
  object Model:
    val components: Lens[Model, Set[ComponentId]] =
      Lens[Model, Set[ComponentId]](_.components)(a => _.copy(components = a))

    def componentAt(id: ComponentId): Lens[Model, Boolean] =
      components.andThen(Monocle.at[Set[ComponentId], ComponentId, Boolean](id))

    val filter: Lens[Model, String] =
      Lens[Model, Option[String]](_.filter)(a => _.copy(filter = a))
        .andThen(Lenses.emptyString)

  // -- View

  def render(
      model: SignallingRef[IO, Model],
      components: Signal[IO, Map[ComponentType, List[ComponentWithDevice]]],
      totals: Map[ComponentId, Distance],
      zoneId: ZoneId
  ): Resource[IO, HtmlDivElement[IO]] =
    val componentList = (model, components).mapN(_.applyFilter(_))
    val data = (componentList, model.map(_.components)).mapN(_ -> _)
    div(
      cls := Css.flexCol + Css("w-full"),
      div(
        cls := Css.flexRowCenter + Css("mb-2"),
        input.withSelf { self =>
          (
            cls := Css.textInput,
            placeholder := "Search components…",
            autoFocus := true,
            onKeyDown --> {
              _.filter(_.key == KeyValue.Enter)
                .evalMap(_ => self.value.get)
                .foreach { str =>
                  model.update(Model.filter.replace(str))
                }
            }
          )
        },
        model
          .map(_.components)
          .changes
          .map(ids =>
            div(
              cls := Css("ml-2").showWhen(ids.nonEmpty),
              s"${ids.size} components selected!"
            )
          )
      ),
      div(
        cls := Css("max-h-96 overflow-auto"),
        table(
          cls := Css.tableAuto,
          thead(
            cls := Css.tableHead + Css("sticky top-0"),
            tr(
              cls := Css.tableHeadRow,
              th(cls := Css.tableHeadCell),
              th(cls := Css.tableHeadCell),
              th(cls := Css.tableHeadCell, "Name"),
              th(cls := Css.tableHeadCellMd, "Added")
            )
          ),
          data.changes
            .map { case (cs, selected) =>
              tbody(
                cs.map(p =>
                  tr(
                    cls := Css.tableRow + Css(
                      "cursor-pointer dark:hover:bg-slate-600/20"
                    ),
                    onClick --> Action
                      .eval(
                        model.update(Model.componentAt(p.id).modify(!_))
                      )
                      .pipe,
                    td(
                      cls := Css.tableRowCell,
                      div(
                        cls := Css("text-center w-6 mx-auto text-green-500 text-2xl"),
                        span(
                          cls := Css("fa fa-check").visibleWhen(selected.contains(p.id))
                        )
                      )
                    ),
                    td(
                      cls := Css.tableRowCell,
                      ComponentIcon(
                        p.component.product.productType,
                        cls := "h-8 w-8 mx-auto"
                      )
                    ),
                    td(
                      cls := Css.tableRowCell,
                      span(p.component.component.name),
                      span(
                        cls := Css("ml-2 text-xs"),
                        "(",
                        p.component.brand.name,
                        " ",
                        p.component.product.name,
                        ")"
                      ),
                      p.device.map { d =>
                        span(
                          cls := Css(
                            "ml-2 text-sm italic ml-2 dark:text-yellow-600 text-orange-600"
                          ),
                          s"Mounted on ${d.brand.name} ${d.device.name}"
                        )
                      },
                      totals.get(p.id).map { dst =>
                        span(
                          cls := Css("ml-2 italic text-sm"),
                          dst.show
                        )
                      }
                    ),
                    td(
                      cls := Css.tableRowCellMd,
                      FormatDate(p.component.component.addedAt, zoneId)
                    )
                  )
                )
              )
            }
        )
      )
    )
}
