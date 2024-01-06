package keeper.webview.client.shared

import cats.Eq
import cats.effect.{IO, Resource}
import cats.syntax.all.*
import fs2.concurrent.SignallingRef
import fs2.dom.HtmlDivElement

import keeper.client.data.FetchResult
import keeper.webview.client.util.Action

import calico.html.io.{*, given}
import monocle.Lens
import org.scalajs.dom.KeyValue

object QuerySelect {
  type MakeLabel[A] = A => String
  type SearchAction[A] = String => IO[FetchResult[List[A]]]

  final case class Model[A](
      query: String = "",
      searchResult: List[A] = Nil,
      menuOpen: Boolean = false,
      searchInProgress: Boolean = false,
      value: Option[A] = None
  ):
    def setValue(v: Option[A], makeLabel: MakeLabel[A]): Model[A] =
      copy(
        query = v.map(makeLabel).getOrElse(""),
        searchInProgress = false,
        searchResult = v.toList,
        value = v
      )

  object Model:
    def empty[A]: Model[A] = Model()

  private def query[A]: Lens[Model[A], String] =
    Lens[Model[A], String](_.query)(a => _.copy(query = a))

  private def menuToggle[A]: Lens[Model[A], Boolean] =
    Lens[Model[A], Boolean](_.menuOpen)(a => _.copy(menuOpen = a))

  private def searchProgress[A]: Lens[Model[A], Boolean] =
    Lens[Model[A], Boolean](_.searchInProgress)(a => _.copy(searchInProgress = a))

  private def searchResult[A]: Lens[Model[A], List[A]] =
    Lens[Model[A], List[A]](_.searchResult)(a => _.copy(searchResult = a))

  private def selectedValue[A]: Lens[Model[A], Option[A]] =
    Lens[Model[A], Option[A]](_.value)(a => _.copy(value = a))

  trait Render[A]:
    def render(
        model: SignallingRef[IO, Model[A]],
        inputId: String,
        fieldLabel: String
    ): Resource[IO, HtmlDivElement[IO]]

  def create[A: Eq](
      makeLabel: MakeLabel[A],
      search: SearchAction[A]
  ): Render[A] =
    new Render[A]:
      override def render(
          model: SignallingRef[IO, Model[A]],
          inputId: String,
          fieldLabel: String
      ): Resource[IO, HtmlDivElement[IO]] =
        div(
          cls := Css.formField,
          onBlur --> Action.eval(model.update(menuToggle.replace(false))).pipe,
          label(cls := Css.inputLabel, forId := inputId, fieldLabel),
          div(
            cls := Css.relative,
            input.withSelf { in =>
              (
                cls := Css.textInput + Css.relative + Css("pl-8 w-full"),
                idAttr := inputId,
                value <-- model.map(query.get).changes,
                nameAttr := inputId,
                onInput --> Action
                  .eval(
                    in.value.get
                      .flatMap(str =>
                        model
                          .update(query.replace(str)) >> searchCandidates(model, search)
                      )
                  )
                  .pipe,
                onFocus --> Action.all(
                  Action.eval(searchCandidates(model, search)),
                  Action.eval(model.update(menuToggle.replace(true)))
                ),
                onKeyDown --> (_.filter(_.key == KeyValue.Enter)
                  .evalTap(_.stopPropagation)
                  .evalTap(_.preventDefault)
                  .foreach(_ =>
                    model.map(searchResult.get).get.flatMap {
                      case head :: Nil =>
                        setSelection(
                          model,
                          head.some,
                          makeLabel
                        )
                      case _ => IO.unit
                    }
                  ))
              )
            },
            model.map(selectedValue.get).changes.map {
              case Some(_) =>
                i(cls := Css.greenCheckIcon + Css("absolute top-3 left-2"))
              case None => span(cls := Css.hidden)
            }
          ),
          div(
            cls <-- model
              .map(menuToggle.get)
              .changes
              .map(open => Css.relative.showWhen(open)),
            model
              .map(searchResult.get)
              .changes
              .map(elements =>
                div(
                  cls := Css.dropDownMenu,
                  elements.map(b =>
                    a(
                      cls := Css.dropDownMenuEntry,
                      makeLabel(b),
                      onClick --> (_.foreach(_ =>
                        setSelection(
                          model,
                          b.some,
                          makeLabel
                        )
                      ))
                    )
                  )
                )
              )
          )
        )

  private def setSelection[M, A](
      model: SignallingRef[IO, Model[A]],
      v: Option[A],
      makeLabel: MakeLabel[A]
  ) =
    model.update(
      menuToggle
        .replace(false)
        .andThen(query.replace(v.map(makeLabel).getOrElse("")))
        .andThen(selectedValue.replace(v))
    )

  private def searchCandidates[A](
      model: SignallingRef[IO, Model[A]],
      searchAction: SearchAction[A]
  ): IO[Unit] =
    model
      .updateAndGet(searchProgress.replace(true))
      .map(_.query)
      .flatMap(searchAction)
      .flatMap {
        case FetchResult.Success(res) =>
          model.update(searchResult.replace(res).andThen(searchProgress.replace(false)))
        case FetchResult.RequestFailed(_) => model.update(searchProgress.replace(false))
      }
}
