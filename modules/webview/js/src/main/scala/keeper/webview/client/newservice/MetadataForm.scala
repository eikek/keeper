package keeper.webview.client.newservice

import cats.effect.*
import cats.syntax.all.*
import fs2.concurrent.SignallingRef
import fs2.dom.HtmlDivElement

import keeper.client.KeeperClient
import keeper.webview.client.shared.{Css, ErrorMessage}
import keeper.webview.client.util.Action

import calico.html.io.{*, given}

object MetadataForm {

  def render(
      model: SignallingRef[IO, MetadataModel],
      client: KeeperClient[IO],
      next: IO[Unit]
  ): Resource[IO, HtmlDivElement[IO]] =
    val nameError = model.map(_.nameValidated.fold(_.head.some, _ => None)).changes
    val dateTimeError =
      model
        .map(_.dateTimeValidated.fold(_ => "The date-time is not valid".some, _ => None))
        .changes

    div(
      cls := Css.flexCol,
      p(
        cls := Css("text-lg my-2"),
        "A maintenance has some title and a date it happened."
      ),
      div(
        cls := Css.form,
        div(
          cls := Css.firstFormField,
          label(cls := Css.inputLabel, forId := "input-name", "Title"),
          input.withSelf { in =>
            (
              cls <-- nameError.map(errs =>
                Css.textInput + Css.errorBorder.when(errs.isDefined)
              ),
              value <-- model.map(MetadataModel.name.get).changes,
              idAttr := "input-name",
              required := true,
              onInput --> Action
                .eval(
                  in.value.get.flatMap(str =>
                    model.update(MetadataModel.name.replace(str))
                  )
                )
                .pipe
            )
          },
          ErrorMessage(nameError)
        ),
        div(
          cls := Css.formField,
          label(cls := Css.inputLabel, forId := "input-date", "Date and Time"),
          div(
            cls <-- dateTimeError.map(errs =>
              Css.flexRowCenter + Css("max-w-fit rounded") + Css.errorBorder.when(
                errs.isDefined
              )
            ),
            input.withSelf { in =>
              (
                cls := Css.textInput + Css("mr-2"),
                typ := "date",
                idAttr := "input-date",
                required := true,
                value <-- model.map(MetadataModel.date.get).changes,
                onInput --> Action
                  .eval(
                    in.value.get.flatMap(str =>
                      model.update(MetadataModel.date.replace(str))
                    )
                  )
                  .pipe
              )
            },
            input.withSelf { in =>
              (
                cls := Css.textInput,
                typ := "time",
                idAttr := "input-time",
                required := true,
                value <-- model.map(MetadataModel.time.get).changes,
                onInput --> Action
                  .eval(
                    in.value.get.flatMap(str =>
                      model.update(MetadataModel.time.replace(str))
                    )
                  )
                  .pipe
              )
            }
          ),
          ErrorMessage(dateTimeError)
        ),
        div(
          cls := Css.formField,
          label(cls := Css.inputLabel, forId := "input-description", "Description"),
          textArea.withSelf { in =>
            (
              cls := Css.textAreaInput,
              value <-- model.map(MetadataModel.description.get).changes,
              idAttr := "input-description",
              onInput --> Action
                .eval(
                  in.value.get.flatMap(str =>
                    model.update(MetadataModel.description.replace(str))
                  )
                )
                .pipe
            )
          }
        ),
        div(
          cls := Css.flexRowCenter + Css("space-x-2"),
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
          button(
            cls := Css.formResetButton,
            typ := "reset",
            href := "#",
            onClick --> Action.all(
              Action.noDefault,
              Action.eval(model.set(MetadataModel()))
            ),
            "Reset"
          )
        )
      )
    )

}
