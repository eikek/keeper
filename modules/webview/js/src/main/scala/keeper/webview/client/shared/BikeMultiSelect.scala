package keeper.webview.client.shared

import scala.collection.immutable.SortedSet

import cats.Eq
import cats.data.NonEmptySet
import cats.effect.*
import cats.syntax.all.*
import fs2.concurrent.{Signal, SignallingRef}
import fs2.dom.HtmlDivElement

import keeper.bikes.model.Bike
import keeper.core.DeviceId
import keeper.webview.client.util.Action

import calico.html.io.{*, given}
import monocle.{Lens, Monocle}

object BikeMultiSelect {

  // -- Model

  final case class Model(
      bikes: Set[DeviceId] = Set.empty
  ) {
    def selectedBikes =
      NonEmptySet.fromSet(SortedSet.from(bikes))
  }
  object Model:
    given Eq[Model] = Eq.fromUniversalEquals

    val bikes: Lens[Model, Set[DeviceId]] =
      Lens[Model, Set[DeviceId]](_.bikes)(a => _.copy(bikes = a))

    def bikeAt(id: DeviceId): Lens[Model, Boolean] =
      bikes.andThen(Monocle.at[Set[DeviceId], DeviceId, Boolean](id))

  // --- View

  def render(
      model: SignallingRef[IO, Model],
      bikesSig: Signal[IO, List[Bike]]
  ): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := Css.flexCol + Css("mt-1"),
      bikesSig.changes.map { bikes =>
        div(
          cls := Css.flexCol,
          bikes.map { comp =>
            a(
              href := "#",
              onClick --> Action.all(
                Action.eval(model.update(Model.bikeAt(comp.id).modify(!_)))
              ),
              cls <-- model.changes.map { m =>
                Css
                  .of(
                    Css.flexRowCenter,
                    Css("space-x-2 block cursor-pointer"),
                    Css("dark:text-lime-500 text-blue-500").when(
                      m.bikes.contains(comp.id)
                    )
                  )
              },
              div(
                cls := Css("w-6 h-6"),
                span(
                  cls <-- model.changes.map(m =>
                    Css.of(
                      Css("fa fa-square font-thin").when(
                        !m.bikes.contains(comp.id)
                      ),
                      Css("fa fa-square-check").when(m.bikes.contains(comp.id))
                    )
                  )
                )
              ),
              div(
                span(
                  cls := Css("text-lg font-semibold"),
                  comp.brand.name,
                  " ",
                  comp.name
                )
              )
            )
          }
        )
      }
    )
}
