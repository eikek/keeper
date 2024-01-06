package keeper.webview.client.newservice

import java.time.temporal.ChronoUnit
import java.time.{Instant, ZoneId}

import cats.data.ValidatedNel
import cats.effect.*
import cats.kernel.Eq
import cats.syntax.all.*
import fs2.concurrent.{Signal, SignallingRef}
import fs2.dom.HtmlDivElement

import keeper.bikes.model.BikeBuilds
import keeper.client.KeeperClient
import keeper.common.Lenses.syntax.*
import keeper.webview.client.View
import keeper.webview.client.cmd.{UiEvent, UiEventDispatch}
import keeper.webview.client.dashboard.BikeDiv
import keeper.webview.client.shared.Css
import keeper.webview.client.util.Action

import calico.html.io.{*, given}
import monocle.Monocle

object ServiceForm {
  private[this] val logger = scribe.cats.io

  def render(
      model: SignallingRef[IO, Model],
      client: KeeperClient[IO],
      cr: UiEventDispatch[IO],
      zoneId: ZoneId
  ): Resource[IO, HtmlDivElement[IO]] =
    val date = model.map(_.metadata.dateTimeValidated.map(_.atZone(zoneId).toInstant))
    setCurrentDate(model, zoneId).toResource >>
      watchService(model, client, zoneId).background >>
      div(
        cls := Css.flexCol + Css("mx-4"),
        h1(
          cls := Css.firstHeadline,
          "New Maintenance"
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
                Action.eval(setView(model, Model.View.BikeTotals))
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
        div(
          cls := Css.flexRow + Css("space-x-2"),
          div(
            cls := Css("grow"),
            model.map(_.view).changes.map {
              case Model.View.Metadata =>
                MetadataForm.render(
                  model.to(Model.metadata),
                  client,
                  fetchBikes(model, client, date).take(1).compile.drain >> setView(
                    model,
                    Model.View.BikeTotals
                  )
                )
              case Model.View.BikeTotals =>
                BikeTotalsForm.render(
                  model.to(Model.totals),
                  model.map(_.bikes.bikes),
                  client,
                  date.get,
                  setView(model, Model.View.Config),
                  setView(model, Model.View.Metadata)
                )
              case Model.View.Config =>
                ServiceEventForm.render(
                  model.to(Model.serviceEvents),
                  model.map(_.recentBuilds),
                  model.map(_.components),
                  zoneId
                )
            }
          ),
          div(
            cls <-- model
              .map(_.view == Model.View.Config)
              .changes
              .map(vis => (Css.flexCol + Css("w-0 lg:w-1/3")).showWhen(vis)),
            div(cls := Css.secondHeadline, "Preview"),
            bikePreview(model)
          )
        )
      )

  def setView(model: SignallingRef[IO, Model], view: Model.View) =
    model.update(Model.view.replace(view))

  def setCurrentDate(model: SignallingRef[IO, Model], zoneId: ZoneId) =
    for {
      // js can't use Clock[IO] operations
      now <- IO(Instant.now().truncatedTo(ChronoUnit.MINUTES))
      ldt = now.atZone(zoneId).toLocalDateTime
      mdate = Model.metadata.andThen(MetadataModel.date)
      mtime = Model.metadata.andThen(MetadataModel.time)
      _ <- logger.debug(s"Set current time $now into maintenance form")
      _ <- model.update(
        mdate
          .replace(ldt.toLocalDate.toString)
          .andThen(mtime.replace(ldt.toLocalTime.toString))
      )
    } yield ()

  def bikePreview(model: SignallingRef[IO, Model]) =
    model.map(_.recentBikesValidated).changes.map {
      case Left(err) =>
        div(
          cls := Css.errorText,
          p(cls := Css("px-2 text-lg overflow-scroll"), err.message),
          ul(cls := Css("list-disc list-inside ml-3 my-2"), err.errors)
        )
      case Right(bikes) =>
        val open = SignallingRef[IO].of(bikes.map(_.id).toSet)
        div(
          cls := Css.flexCol.showWhen(bikes.nonEmpty),
          open.toResource
            .flatMap(sig =>
              bikes.traverse(b =>
                BikeDiv(
                  b,
                  sig.to(Monocle.at(b.id)),
                  model.map(_.totals.totals.get(b.id)),
                  model.map(_.bikes.componentTotals),
                  BikeDiv.Size.sm
                )
              )
            )
        )
    }

  def submit(
      model: SignallingRef[IO, Model],
      client: KeeperClient[IO],
      cr: UiEventDispatch[IO],
      zoneId: ZoneId
  ) =
    model.get.map(_.asBikeService(zoneId)).flatMap {
      _.fold(
        errs => logger.error(s"Invalid bike service: $errs"),
        bs =>
          client
            .submitService(bs)
            .flatMap(
              _.fold(
                _ => model.set(Model()) >> cr.send(UiEvent.SetView(View.Dashboard)),
                errs => logger.error(s"Error submitting service: $errs")
              )
            )
      )
    }

  def fetchBikes(
      model: SignallingRef[IO, Model],
      client: KeeperClient[IO],
      time: Signal[IO, ValidatedNel[String, Instant]]
  ) =
    time
      .changes(Eq.fromUniversalEquals[ValidatedNel[String, Instant]])
      .discrete
      .evalMap(
        _.fold(
          errs => logger.warn(s"Invalid date-time: $errs"),
          ts => {
            val fetchBikes = client
              .getBikesAt(ts)
              .flatMap(
                _.fold(
                  res => model.update(Model.bikeBuilds.replace(res)),
                  err => logger.error(s"Error fetching bikes: $err")
                )
              )
            val fetchComps = client
              .getComponentsAt(ts)
              .flatMap(
                _.fold(
                  res => model.update(Model.componentList.replace(res)),
                  err => logger.error(s"Error fetching components: $err")
                )
              )

            IO.parSequenceN(2)(List(fetchBikes, fetchComps)).void
          }
        )
      )

  def watchService(
      model: SignallingRef[IO, Model],
      client: KeeperClient[IO],
      zoneId: ZoneId
  ) =
    model
      .map(_.asBikeService(zoneId))
      .changes
      .discrete
      .evalMap {
        _.fold(
          _ => IO.unit,
          bs =>
            if (bs.events.isEmpty)
              model.update(Model.preview.replace(Right(BikeBuilds.empty)))
            else
              client
                .previewService(bs)
                .flatMap(res => model.update(Model.preview.replace(res.toEither)))
        )
      }
      .compile
      .drain
}
