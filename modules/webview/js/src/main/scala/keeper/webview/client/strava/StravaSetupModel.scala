package keeper.webview.client.strava

import keeper.client.data.StravaConnectState

import monocle.Lens

final case class StravaSetupModel(
    state: StravaConnectState = StravaConnectState(false, false)
)

object StravaSetupModel:
  val state: Lens[StravaSetupModel, StravaConnectState] =
    Lens[StravaSetupModel, StravaConnectState](_.state)(a => _.copy(state = a))
