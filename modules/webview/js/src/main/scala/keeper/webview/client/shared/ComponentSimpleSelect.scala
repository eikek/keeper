package keeper.webview.client.shared

import cats.Eq
import cats.effect.*
import cats.syntax.all.*
import fs2.concurrent.{Signal, SignallingRef}
import fs2.dom.HtmlDivElement

import keeper.bikes.data.ComponentWithDevice
import keeper.common.Distance
import keeper.core.ComponentId
import keeper.webview.client.util.Action

import calico.html.io.{*, given}
import monocle.{Lens, Monocle}

object ComponentSimpleSelect {

  // -- Model

  final case class Model(
      components: Set[ComponentId] = Set.empty
  )
  object Model:
    given Eq[Model] = Eq.fromUniversalEquals

    val components: Lens[Model, Set[ComponentId]] =
      Lens[Model, Set[ComponentId]](_.components)(a => _.copy(components = a))

    def componentAt(id: ComponentId): Lens[Model, Boolean] =
      components.andThen(Monocle.at[Set[ComponentId], ComponentId, Boolean](id))

  // --- View

  def render(
      model: SignallingRef[IO, Model],
      components: Signal[IO, List[ComponentWithDevice]],
      totals: Map[ComponentId, Distance]
  ): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := Css.flexCol + Css("mt-1"),
      components.changes.map { comps =>
        div(
          cls := Css.flexCol,
          comps.map { comp =>
            a(
              href := "#",
              onClick --> Action.all(
                Action.eval(model.update(Model.componentAt(comp.id).modify(!_)))
              ),
              cls <-- model.changes.map { m =>
                Css
                  .of(
                    Css.flexRowCenter,
                    Css("space-x-2 block cursor-pointer"),
                    Css("dark:text-lime-500 text-blue-500").when(
                      m.components.contains(comp.id)
                    )
                  )
              },
              div(
                cls := Css("w-6 h-6"),
                span(
                  cls <-- model.changes.map(m =>
                    Css.of(
                      Css("fa fa-square font-thin").when(
                        !m.components.contains(comp.id)
                      ),
                      Css("fa fa-square-check").when(m.components.contains(comp.id))
                    )
                  )
                )
              ),
              div(
                span(
                  cls := Css("text-lg font-semibold"),
                  comp.component.component.name
                ),
                span(cls := "ml-2 ", "(", comp.component.brand.name),
                span(cls := "ml-1", comp.component.product.name, ")")
              ),
              comp.device.map { dev =>
                div(
                  cls := Css(
                    "italic ml-2 dark:text-yellow-600 text-orange-600"
                  ),
                  "Currently on ",
                  dev.brand.name,
                  " ",
                  dev.device.name,
                  "!"
                )
              },
              totals.get(comp.id).map { dst =>
                div(
                  cls := Css("ml-2 italic text-sm"),
                  dst.show
                )
              }
            )
          }
        )
      }
    )
}
