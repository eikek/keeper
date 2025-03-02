package keeper.webview.client.newbike

import java.time.temporal.ChronoUnit
import java.time.{Instant, ZoneId}

import cats.data.Validated
import cats.effect.*
import cats.syntax.all.*
import fs2.concurrent.SignallingRef
import fs2.dom.HtmlDivElement

import keeper.client.KeeperClient
import keeper.client.data.FetchResult
import keeper.common.Lenses.syntax.*
import keeper.webview.client.View
import keeper.webview.client.cmd.{UiEvent, UiEventDispatch}
import keeper.webview.client.shared.Css
import keeper.webview.client.util.Action

import calico.html.io.{*, given}

object NewBikeForm {
  private val logger = scribe.cats.io

  def render(
      model: SignallingRef[IO, Model],
      client: KeeperClient[IO],
      cr: UiEventDispatch[IO],
      zoneId: ZoneId
  ): Resource[IO, HtmlDivElement[IO]] =
    setCurrentDate(model, zoneId).toResource >>
      div(
        cls := Css.flexCol + Css("container mx-auto"),
        h1(
          cls := Css.firstHeadline,
          "New Bike!!"
        ),
        div(
          cls <-- model
            .map(_.metadata)
            .map(m => m.nameValidated.isValid && m.dateTimeValidated.isValid)
            .map { visible =>
              Css.flexRowCenter.showWhen(visible)
            },
          model.map(_.metadata).map(m => s"New Bike Day on ${m.date} at ${m.time}")
        ),
        div(
          cls <-- model
            .map(_.view == Model.View.Config)
            .changes
            .map(flag => Css.flexRowCenter.showWhen(flag)),
          div(cls := Css("grow")),
          div(
            a(
              href := "#",
              cls := Css.formResetButton + Css("mr-2"),
              onClick --> Action.all(
                Action.eval(model.update(Model.view.replace(Model.View.Metadata)))
              ),
              "Back"
            ),
            a(
              cls := Css.formSubmitButton,
              disabled <-- model.map(_.submitInProgress).changes,
              href := "#",
              onClick --> Action.all(
                Action.noDefault,
                Action.eval(submit(model, client, cr, zoneId))
              ),
              model.map(_.submitInProgress).changes.map { flag =>
                if (flag)
                  i(cls := Css.loadingSpinner + Css.mr2)
                else
                  i(cls := Css.uploadIcon + Css.mr2)
              },
              "Submit"
            )
          )
        ),
        model.map(_.view).changes.map {
          case Model.View.Metadata =>
            MetadataForm.render(
              model.to(Model.metadata),
              client,
              model.update(Model.view.replace(Model.View.Config))
            )
          case Model.View.Config =>
            ConfigForm.render(
              model.to(Model.config),
              client,
              model.map(_.getDate.map(_.atZone(zoneId).toInstant)).get
            )
        }
      )

  def setCurrentDate(model: SignallingRef[IO, Model], zoneId: ZoneId) =
    for {
      // js can't use Clock[IO] operations
      now <- IO(Instant.now().truncatedTo(ChronoUnit.MINUTES))
      ldt = now.atZone(zoneId).toLocalDateTime
      mdate = Model.metadata.andThen(MetadataModel.date)
      mtime = Model.metadata.andThen(MetadataModel.time)

      _ <- model.update(
        mdate
          .replace(ldt.toLocalDate.toString)
          .andThen(mtime.replace(ldt.toLocalTime.toString))
      )
    } yield ()

  def submit(
      model: SignallingRef[IO, Model],
      client: KeeperClient[IO],
      dispatch: UiEventDispatch[IO],
      zoneId: ZoneId
  ) =
    model.get.map(_.asBikeService(zoneId)).flatMap {
      case Validated.Valid(s) =>
        client.submitService(s).flatMap {
          case FetchResult.RequestFailed(err) =>
            logger.error(s"Submitting bike service failed: $err")

          case FetchResult.Success(_) =>
            dispatch.send(UiEvent.SetView(View.Dashboard)) >>
              model.set(Model())
        }
      case Validated.Invalid(e) =>
        logger.error(s"Invalid bike service data: $e")
    }
}
