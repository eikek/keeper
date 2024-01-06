package keeper.bikes.fit4s

import scala.concurrent.duration.*

import org.http4s.Uri

final case class Fit4sConfig(
    baseUrl: Uri,
    timeout: Duration
)
