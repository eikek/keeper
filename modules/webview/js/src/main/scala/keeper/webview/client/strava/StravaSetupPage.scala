package keeper.webview.client.strava

import cats.effect.{IO, Resource}
import cats.syntax.all.*
import fs2.concurrent.SignallingRef
import fs2.dom.HtmlDivElement

import keeper.client.KeeperClient
import keeper.client.data.FetchResult
import keeper.webview.client.BaseUrl
import keeper.webview.client.shared.Css

import calico.html.io.{*, given}

object StravaSetupPage {
  private[this] val logger = scribe.cats.io

  def render(
      model: SignallingRef[IO, StravaSetupModel],
      client: KeeperClient[IO],
      baseUrl: BaseUrl
  ): Resource[IO, HtmlDivElement[IO]] =
    getState(model, client).toResource >>
      div(
        cls := Css("container mx-auto"),
        h1(
          cls := Css.firstHeadline,
          "Connect Strava"
        ),
        p(
          cls := Css("py-2"),
          "Connect your strava account to use it for finding the current distances of your bikes."
        ),
        p(
          cls := Css("py-2"),
          "Of course, you can always just type it in. For dates in the past, Strava cannot be used, though. ",
          "It can only be used to get the current distances of your bikes. It also requires to use same names ",
          "of your bikes here and at Strava."
        ),
        model.map(_.state).changes.map { state =>
          if (!state.enabled) notConfiguredMessage
          else if (state.valid) alreadyDoneMessage
          else connectButton(baseUrl)
        }
      )

  def connectButton(baseUrl: BaseUrl) =
    div(
      cls := Css.flexCol + Css("py-4 items-center"),
      a(
        href := s"${baseUrl.get.renderString}/strava/connect",
        target := "_new",
        cls := Css.formResetButton + Css("block text-xl"),
        span(cls := "fa-brands fa-strava mr-2"),
        "Connect to Strava"
      )
    )

  def alreadyDoneMessage =
    div(
      cls := Css.flexCol + Css("space-y-2 mt-4") + Css.infoText,
      p(
        "Strava connection is set up fine!"
      )
    )

  def notConfiguredMessage =
    div(
      cls := Css.flexCol + Css("space-y-2 mt-4") + Css.infoText,
      p(
        "The Strava connection is not configured at the server. You need to enable it by ",
        "setting up environment variables for client-id and client-secret."
      )
    )

  def getState(model: SignallingRef[IO, StravaSetupModel], client: KeeperClient[IO]) =
    client.getStravaConnectState.flatMap {
      case FetchResult.Success(state) =>
        model.update(StravaSetupModel.state.replace(state))
      case FetchResult.RequestFailed(err) =>
        logger.error(s"Error getting strava connect state: $err")
    }
}
