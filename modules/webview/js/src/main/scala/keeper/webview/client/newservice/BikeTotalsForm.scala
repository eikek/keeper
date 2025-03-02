package keeper.webview.client.newservice

import java.time.Instant

import cats.data.ValidatedNel
import cats.effect.*
import cats.syntax.all.*
import fs2.concurrent.{Signal, SignallingRef}
import fs2.dom.HtmlDivElement

import keeper.bikes.model.Bike
import keeper.client.KeeperClient
import keeper.webview.client.shared.{Css, ErrorMessage}
import keeper.webview.client.util.Action

import calico.html.io.{*, given}

object BikeTotalsForm {
  private val logger = scribe.cats.io

  def render(
      model: SignallingRef[IO, BikeTotalsModel],
      bikes: Signal[IO, List[Bike]],
      client: KeeperClient[IO],
      time: IO[ValidatedNel[String, Instant]],
      next: IO[Unit],
      back: IO[Unit]
  ): Resource[IO, HtmlDivElement[IO]] =
    val errorList = model.map(_.totalsValidated.fold(_.toList, _ => Nil))
    getInitialData(model, client, time, false).background >>
      div(
        cls := Css.flexCol,
        p(
          cls := Css("text-lg my-2"),
          "In order to calculate how far every component travelled, it is necessary to record the total distance of each bike."
        ),
        bikes.changes
          .map { bikes =>
            div(
              cls := Css.form + Css("relative"),
              disabled <-- model.map(_.loadingData).changes,
              div(
                cls <-- model
                  .map(_.loadingData)
                  .changes
                  .map(loading =>
                    Css("w-10 h-10 absolute right-2 top-4 mx-auto").showWhen(loading)
                  ),
                span(cls := Css.loadingSpinner)
              ),
              bikes.map(b =>
                div(
                  cls := Css.formField + Css("relative"),
                  label(
                    cls := Css.inputLabel,
                    forId := s"distance-${b.id}",
                    b.brand.name,
                    " ",
                    b.name
                  ),
                  input.withSelf { in =>
                    (
                      cls := Css.textInput + Css("ml-2 w-8/12 pl-8"),
                      disabled <-- model.map(_.loadingData).changes,
                      idAttr := s"distance-${b.id}",
                      nameAttr := s"distance-${b.id}",
                      value <-- model.map(BikeTotalsModel.totalsOf(b.id).get),
                      onInput -->
                        Action
                          .eval(
                            in.value.get.flatMap(str =>
                              model.update(BikeTotalsModel.totalsOf(b.id).replace(str))
                            )
                          )
                          .pipe
                    )
                  },
                  div(
                    cls <-- model
                      .map(_.loadingData)
                      .changes
                      .map(loading =>
                        Css("w-8 h-8 absolute bottom-3 left-4") + Css.textColorViz
                          .when(!loading)
                      ),
                    span(cls := Css("fa fa-arrow-right"))
                  )
                )
              ),
              ErrorMessage.list(errorList),
              div(
                cls := Css.flexRowCenter + Css("space-x-2 mt-3"),
                button(
                  cls := Css.formResetButton,
                  href := "#",
                  onClick --> Action.all(
                    Action.noDefault,
                    Action.eval(back)
                  ),
                  "← Back"
                ),
                button(
                  cls := Css.formSubmitButton,
                  disabled <-- model.map(_.isInvalid).changes,
                  href := "#",
                  onClick --> Action.all(
                    Action.noDefault,
                    Action.eval(next)
                  ),
                  "Next →"
                ),
                div(cls := "grow"),
                button(
                  cls := Css.formResetButton,
                  typ := "reset",
                  href := "#",
                  onClick --> Action.all(
                    Action.noDefault,
                    Action.eval(getInitialData(model, client, time, true))
                  ),
                  "Reset"
                )
              )
            )
          }
      )

  def getInitialData(
      model: SignallingRef[IO, BikeTotalsModel],
      client: KeeperClient[IO],
      timeIO: IO[ValidatedNel[String, Instant]],
      force: Boolean
  ) =
    timeIO.flatMap(
      _.fold(
        _ => IO.unit,
        time =>
          model.get.map(_.totals.isEmpty || force).flatMap {
            case false => IO.unit
            case true =>
              BikeTotalsModel.withLoading[IO](model).use { _ =>
                client.getDeviceTotals(Some(time)).flatMap {
                  _.fold(
                    to =>
                      model.update(
                        BikeTotalsModel.totals
                          .replace(to.map(b => b.bikeId -> b.distance).toMap)
                      ),
                    errs => logger.error(s"Bikes or totals request failed: $errs")
                  )
                }
              }
          }
      )
    )
}
