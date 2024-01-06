package keeper.bikes

import java.time.Instant

import keeper.bikes.event.ServiceEvent
import keeper.bikes.model.{BikeBuilds, BikeService, BikeServiceError}

trait BikeServiceProvider[F[_]] {

  def processBikeService(service: BikeService): F[Either[BikeServiceError, BikeBuilds]]

  def previewBikeService(
      date: Instant,
      serviceEvents: List[ServiceEvent]
  ): F[Either[BikeServiceError, BikeBuilds]]
}
