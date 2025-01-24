package keeper.webview.client.brands

import java.time.ZoneId

import cats.data.Validated
import cats.effect.{IO, Resource}
import cats.syntax.all.*
import fs2.concurrent.SignallingRef
import fs2.dom.{HtmlDivElement, HtmlElement}

import keeper.bikes.*
import keeper.bikes.data.*
import keeper.client.KeeperClient
import keeper.client.data.FetchResult
import keeper.webview.client.cmd.{UiEvent, UiEventDispatch}
import keeper.webview.client.shared.{Css, ErrorMessage}
import keeper.webview.client.util.Action

import calico.html.io.{*, given}

object BrandForm {

  def render(
      model: SignallingRef[IO, FormModel],
      client: KeeperClient[IO],
      dispatch: UiEventDispatch[IO],
      zoneId: ZoneId
  ): Resource[IO, HtmlElement[IO]] =
    val nameError = model.map(_.nameError).changes

    subscribe(model, dispatch).compile.drain.background >>
      div(
        cls := Css.form,
        disabled <-- model.map(_.saveInProgress).changes,
        div(
          cls := Css.firstFormField,
          label(cls := Css.inputLabel, forId := "input-name", "Name"),
          input.withSelf { in =>
            (
              cls <-- nameError.map(errs =>
                Css.textInput + Css.errorBorder.when(errs.isDefined)
              ),
              value <-- model.map(FormModel.name.get).changes,
              idAttr := "input-name",
              required := true,
              onInput --> Action
                .eval(
                  in.value.get.flatMap(str => model.update(FormModel.name.replace(str)))
                )
                .pipe
            )
          },
          ErrorMessage(nameError)
        ),
        div(
          cls := Css.formField,
          label(cls := Css.inputLabel, forId := "input-description", "Description"),
          textArea.withSelf { in =>
            (
              cls := Css.textAreaInput,
              value <-- model.map(FormModel.description.get).changes,
              idAttr := "input-description",
              onInput --> Action
                .eval(
                  in.value.get
                    .flatMap(str => model.update(FormModel.description.replace(str)))
                )
                .pipe
            )
          }
        ),
        div(
          cls := Css.flexRowCenter + Css("space-x-2"),
          button(
            cls := Css.formSubmitButton,
            typ := "submit",
            disabled <-- model.map(_.saveInProgress).changes,
            href := "#",
            onClick --> Action.all(
              Action.noDefault,
              Action.eval(submit(model, client, dispatch))
            ),
            model.map(_.saveInProgress).changes.map { flag =>
              if (flag)
                i(cls := Css.loadingSpinner + Css.mr2)
              else
                i(cls := Css.uploadIcon + Css.mr2)
            },
            "Submit"
          ),
          button(
            cls := Css.formResetButton,
            typ := "reset",
            href := "#",
            onClick --> (_.evalTap(_.preventDefault)
              .evalTap(_.stopPropagation)
              .foreach(_ => model.set(FormModel()))),
            "Reset"
          )
        )
      )

  private def submit(
      model: SignallingRef[IO, FormModel],
      client: KeeperClient[IO],
      dispatch: UiEventDispatch[IO]
  ): IO[Unit] =
    model.get
      .map(_.makeBrand)
      .flatMap {
        case Validated.Valid((id, p)) =>
          model.update(FormModel.saveTrue) >>
            client.createOrUpdateBrand(id, p).flatMap {
              case FetchResult.Success(r) =>
                val event =
                  id.map(UiEvent.BikeBrandUpdated(_, p))
                    .getOrElse(UiEvent.BikeBrandCreated(BrandId(r.id.toInt), p))

                dispatch.send(event) >>
                  scribe.cats.io.info(s"Brand created: $r")

              case FetchResult.RequestFailed(errs) =>
                scribe.cats.io.error(s"Brand creation failed: $errs")
            }

        case Validated.Invalid(e) =>
          scribe.cats.io.warn(s"Errors in brand form: ${e.toList.mkString(", ")}")
      }
      .flatMap(_ => model.update(FormModel.saveFalse))

  private def subscribe(
      model: SignallingRef[IO, FormModel],
      dispatch: UiEventDispatch[IO]
  ) =
    dispatch.subscribe.evalMap {
      case UiEvent.BikeBrandEditRequest(brand) =>
        model.update(_.setBrand(brand))

      case _ =>
        IO.unit
    }
}
