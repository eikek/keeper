package keeper.bikes.db

import java.time.Instant

import keeper.bikes.data.ComponentWithDevice
import keeper.bikes.model.*
import keeper.core.MaintenanceBuild

trait Inventory[F[_]] {

  def brands: BrandRepository[F]

  def products: ProductRepository[F]

  def components: ComponentRepository[F]

  def componentSource: ComponentSource[F]

  def devices: DeviceRepository[F]

  def getCurrentBuilds: F[MaintenanceBuild]

  def getBuildsAt(at: Instant): F[MaintenanceBuild]

  def getCurrentBikes(
      currentTotals: List[BikeTotal]
  ): F[Either[BikesResolveError, BikeBuilds]]

  def getBikesAt(
      at: Instant,
      currentTotals: List[BikeTotal]
  ): F[Either[BikesResolveError, BikeBuilds]]

  def getComponentsAt(at: Instant): F[List[ComponentWithDevice]]
}
