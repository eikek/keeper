package keeper.webview.client.newservice

import cats.effect.*
import cats.syntax.all.*
import fs2.concurrent.{Signal, SignallingRef}
import fs2.dom.HtmlDivElement

import keeper.bikes.data.{ComponentType, ComponentWithDevice}
import keeper.bikes.event.Alter
import keeper.bikes.model.BikePart
import keeper.common.Distance
import keeper.core.{ComponentId, DeviceId}
import keeper.webview.client.icons.ComponentIcon
import keeper.webview.client.shared.Css
import keeper.webview.client.util.Action

import calico.html.io.{*, given}

object BikePartOption {
  def render(
      bike: Option[DeviceId],
      ct: ComponentType,
      current: Option[Option[BikePart]],
      components: Signal[IO, Map[ComponentType, List[ComponentWithDevice]]],
      model: SignallingRef[IO, Alter[ComponentId]],
      showMounted: Signal[IO, Boolean],
      totals: Map[ComponentId, Distance]
  ): Resource[IO, HtmlDivElement[IO]] =
    val options: Signal[IO, List[ComponentWithDevice]] =
      components.map(_.getOrElse(ct, Nil))
    div(
      cls <-- options.map(_.nonEmpty).changes.map(vis => Css.flexCol.showWhen(vis)),
      div(
        cls := Css.of(
          Css.flexRowCenter,
          Css("py-2 px-2"),
          Css.blueBoxed,
          Css.textColorViz
        ),
        ComponentIcon(ct, cls := Css("h-6 w-6 mr-2")),
        current match {
          case Some(Some(c)) =>
            div(
              span(cls := Css("text-lg font-semibold"), c.name),
              span(cls := "ml-2 ", "(", c.product.brand.name),
              span(cls := "ml-1", c.product.product.name, ")")
            )
          case Some(None) =>
            div(cls := Css("text-lg italic"), "Not selected")

          case None =>
            div(cls := Css("text-lg italic"), "No parent component mounted")
        },
        model.changes.map {
          case Alter.Discard =>
            div(cls := Css("ml-2 italic").showWhen(current.isDefined), "Unchanged")
          case Alter.Unset => div(cls := "ml-2 font-bold", "Removed")
          case Alter.Replace(id) =>
            options.get.toResource
              .flatMap(
                _.find(_.component.id == id)
                  .map(toc =>
                    div(
                      cls := "ml-2 font-bold",
                      span(cls := "fa fa-arrow-right mr-2"),
                      s"${toc.component.component.name}"
                    )
                  )
                  .getOrElse(div(cls := Css.hidden))
              )
        }
      ),
      options.map(opts => current.fold(Nil)(_ => opts)).changes.map { opts =>
        div(
          cls := Css.flexCol + Css("ml-4 py-1"),
          opts.map { c =>
            val isCurrent = current.exists(_.exists(_.id == c.id))
            a(
              href := "#",
              cls <-- (model, showMounted).mapN(_ -> _).changes.map {
                case (alter, showM) =>
                  Css
                    .of(
                      Css.flexRowCenter,
                      Css("space-x-2 block cursor-pointer"),
                      Css("dark:text-lime-500 text-blue-500").when(
                        isCurrent && alter.isDiscard
                      ),
                      Css("dark:text-lime-500 text-blue-500").when(
                        !isCurrent && alter.isReplace(c.id)
                      ),
                      Css("text-red-500 line-through").when(isCurrent && !alter.isDiscard)
                    )
                    .showWhen(
                      c.device.isEmpty || c.device
                        .map(_.device.id) == bike || isCurrent || showM
                    )
              },
              onClick --> Action.all(
                Action.eval(model.update {
                  case Alter.Discard =>
                    if (isCurrent) Alter.Unset else Alter.Replace(c.id)
                  case Alter.Unset =>
                    if (isCurrent) Alter.Discard else Alter.Replace(c.id)
                  case Alter.Replace(id) =>
                    if (id == c.id || isCurrent) Alter.Discard else Alter.Replace(c.id)
                })
              ),
              div(
                cls := Css("w-6 h-6"),
                span(
                  cls <-- model.changes.map(alter =>
                    Css.of(
                      Css("fa fa-square font-thin")
                        .when(!isCurrent && !alter.isReplace(c.id)),
                      Css("fa fa-square-xmark").when(isCurrent && !alter.isDiscard),
                      Css("fa fa-square-check").when(
                        (isCurrent && alter.isDiscard) ||
                          (!isCurrent && alter.isReplace(c.id))
                      )
                    )
                  )
                )
              ),
              div(
                span(
                  cls := Css("text-lg font-semibold"),
                  c.component.component.name
                ),
                span(cls := "ml-2 ", "(", c.component.brand.name),
                span(cls := "ml-1", c.component.product.name, ")")
              ),
              c.device.filter(_.device.id.some != bike).map { dev =>
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
              totals.get(c.id).map { dst =>
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
