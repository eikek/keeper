package keeper.webview.client.products

import cats.data.Validated
import cats.effect.{IO, Resource}
import cats.syntax.all.*
import fs2.concurrent.SignallingRef
import fs2.dom.{HtmlDivElement, HtmlElement}

import keeper.bikes.*
import keeper.bikes.data.*
import keeper.client.KeeperClient
import keeper.client.data.FetchResult
import keeper.common.Lenses.syntax.*
import keeper.webview.client.cmd.{UiEvent, UiEventDispatch}
import keeper.webview.client.shared.{Css, ErrorMessage, QuerySelect}
import keeper.webview.client.util.Action

import calico.html.io.{*, given}

object ProductForm {

  private def brandSelect(client: KeeperClient[IO]) =
    QuerySelect.create[Brand](
      _.name,
      query => client.searchBrands(query.some.filter(_.nonEmpty))
    )

  private val componentTypeSelect =
    QuerySelect.create[ComponentType](
      _.name,
      query =>
        FetchResult
          .Success(
            ComponentType.values
              .filter(_.name.toLowerCase.contains(query.toLowerCase))
              .toList
              .sortBy(_.name)
          )
          .pure[IO]
    )

  def render(
      model: SignallingRef[IO, FormModel],
      client: KeeperClient[IO],
      dispatch: UiEventDispatch[IO]
  ): Resource[IO, HtmlElement[IO]] =
    val nameError = model.map(_.nameError).changes
    val weightError = model.map(_.weightError).changes

    subscribe(model, dispatch).compile.drain.background >>
      div(
        cls := Css.form,
        disabled <-- model.map(_.saveInProgress).changes,
        componentTypeSelect
          .render(model.to(FormModel.productTypeSelect), "input-product-type", "Type"),
        brandSelect(client)
          .render(model.to(FormModel.brandSelect), "input-brand", "Brand"),
        div(
          cls := Css.formField,
          label(cls := Css.inputLabel, forId := "input-name", "Name"),
          input.withSelf { in =>
            (
              cls <-- nameError.map(errs =>
                Css.textInput + Css.errorBorder.when(errs.isDefined)
              ),
              value <-- model.map(FormModel.name.get).changes,
              idAttr := "input-name",
              required := true,
              onInput --> Action.all(
                Action.eval(
                  in.value.get.flatMap(str => model.update(FormModel.name.replace(str)))
                )
              )
            )
          },
          ErrorMessage(nameError)
        ),
        div(
          cls := Css.formField,
          label(cls := Css.inputLabel, forId := "input-weight", "Weight (g)"),
          input.withSelf { in =>
            (
              cls <-- weightError.map(errs =>
                Css.textInput + Css.errorBorder.when(errs.isDefined)
              ),
              value <-- model.map(FormModel.weight.get).changes,
              idAttr := "input-weight",
              onInput --> Action
                .eval(
                  in.value.get.flatMap(str => model.update(FormModel.weight.replace(str)))
                )
                .pipe
            )
          },
          ErrorMessage(weightError)
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
                  in.value.get.flatMap(str =>
                    model.update(FormModel.description.replace(str))
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
              .foreach(_ => model.set(FormModel.empty))),
            "Reset"
          )
        )
      )

  private def submit(
      model: SignallingRef[IO, FormModel],
      client: KeeperClient[IO],
      dispatch: UiEventDispatch[IO]
  ) =
    model.get
      .map(_.makeProduct)
      .flatMap {
        case Validated.Valid((id, p)) =>
          model.update(FormModel.saveTrue) >>
            client.createOrUpdateProduct(id, p).flatMap {
              case FetchResult.Success(r) =>
                val event =
                  id.map(UiEvent.BikeProductUpdated(_, p))
                    .getOrElse(UiEvent.BikeProductCreated(ProductId(r.id), p))

                dispatch.send(event) >>
                  scribe.cats.io.info(s"Product created: $r")

              case FetchResult.RequestFailed(errs) =>
                scribe.cats.io.error(s"Product creation failed: $errs")
            }

        case Validated.Invalid(e) =>
          scribe.cats.io.warn(s"Errors in product form: ${e.toList.mkString(", ")}")
      }
      .flatMap(_ => model.update(FormModel.saveFalse))

  private def subscribe(
      model: SignallingRef[IO, FormModel],
      dispatch: UiEventDispatch[IO]
  ) =
    dispatch.subscribe.evalMap {
      case UiEvent.BikeProductEditRequest(p) =>
        model.update(_.setProduct(p))

      case UiEvent.BikeProductCopyRequest(p) =>
        model.update(_.setProduct(p).copy(productId = None))

      case _ =>
        IO.unit
    }
}
