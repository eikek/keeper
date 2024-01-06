package keeper.webview.client.shared

import cats.Eq
import cats.effect.{IO, Resource}
import fs2.concurrent.{Signal, SignallingRef}
import fs2.dom.HtmlDivElement

import keeper.webview.client.util.Action

import calico.html.io.{*, given}

object SimpleSelect {

  type Model[A] = Option[A]

  trait Render[A]:
    def render(
        model: SignallingRef[IO, Model[A]],
        inputId: String,
        fieldLabel: String
    ): Resource[IO, HtmlDivElement[IO]]

  def create[A](
      all: List[A],
      mkLabel: A => String,
      fromString: String => Option[A]
  ): Render[A] =
    (model: SignallingRef[IO, Model[A]], inputId: String, fieldLabel: String) =>
      div(
        cls := Css.firstFormField,
        label(
          cls := Css.inputLabel.showWhen(fieldLabel.nonEmpty),
          forId := inputId,
          fieldLabel
        ),
        select.withSelf { sel =>
          (
            cls := Css.selectInput,
            all.map(ct => option(value := mkLabel(ct), mkLabel(ct))),
            value <-- model.map(_.map(mkLabel).getOrElse("")).changes,
            onChange --> Action
              .eval(
                sel.value.get
                  .map(fromString)
                  .flatMap(model.set)
              )
              .pipe
          )
        }
      )

  def create[A: Eq, B](
      options: Signal[IO, List[A]],
      mkValue: A => B,
      mkLabel: A => String,
      fromValue: String => Option[B]
  ): Render[B] =
    (model: SignallingRef[IO, Model[B]], inputId: String, fieldLabel: String) =>
      div(
        cls := Css.firstFormField,
        label(
          cls := Css.inputLabel.showWhen(fieldLabel.nonEmpty),
          forId := inputId,
          fieldLabel
        ),
        options.changes.map { opts =>
          select.withSelf { sel =>
            (
              cls := Css.selectInput,
              idAttr := inputId,
              opts.map(b => option(value := mkValue(b).toString, mkLabel(b))),
              value <-- model.map(_.map(_.toString).getOrElse("")).changes,
              onChange --> Action.all(
                Action.eval(
                  sel.value.get
                    .map(fromValue)
                    .flatMap(model.set)
                )
              )
            )
          }
        }
      )
}
