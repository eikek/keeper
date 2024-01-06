package keeper.webview.client.newservice

import cats.Eq
import cats.data.ValidatedNel
import cats.syntax.all.*

import keeper.bikes.event.{ServiceEvent, ServiceEventName}

trait AsServiceEvent {
  def eventName: ServiceEventName
  def asServiceEvent: ValidatedNel[String, ServiceEvent]
  def reset: ServiceEventModel => ServiceEventModel
}

object AsServiceEvent:
  given Eq[AsServiceEvent] = Eq.fromUniversalEquals

  def nothing(name: ServiceEventName): AsServiceEvent = new AsServiceEvent:
    def eventName: ServiceEventName = name
    def asServiceEvent: ValidatedNel[String, ServiceEvent] =
      "No event available".invalidNel
    def reset: ServiceEventModel => ServiceEventModel = identity
