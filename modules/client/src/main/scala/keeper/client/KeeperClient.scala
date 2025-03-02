package keeper.client

import java.time.Instant

import keeper.bikes.data.*
import keeper.bikes.model.*
import keeper.bikes.{Page, SimpleQuery}
import keeper.client.data.{CreateResult, FetchResult, StravaConnectState}
import keeper.core.ComponentId

trait KeeperClient[F[_]] {

  def getStravaConnectState: F[FetchResult[StravaConnectState]]

  def getDeviceTotals(at: Option[Instant]): F[FetchResult[List[BikeTotal]]]

  def getServices(until: Option[Instant], page: Page): F[FetchResult[List[BikeService]]]

  def getServiceDetails(mask: ServiceSearchMask): F[FetchResult[List[ServiceDetail]]]

  def previewService(s: BikeService): F[FetchResult[BikeBuilds]]

  def submitService(s: BikeService): F[FetchResult[BikeBuilds]]

  def getCurrentBikes: F[FetchResult[BikeBuilds]]

  def getBikesAt(time: Instant): F[FetchResult[BikeBuilds]]

  def searchBrands(q: Option[String]): F[FetchResult[List[Brand]]]

  def searchProducts(q: SimpleQuery): F[FetchResult[List[ProductWithBrand]]]

  def searchComponents(q: SimpleQuery): F[FetchResult[List[ComponentWithProduct]]]

  def getComponentsAt(time: Instant): F[FetchResult[List[ComponentWithDevice]]]

  def createOrUpdateBrand(
      id: Option[BrandId],
      brand: NewBrand
  ): F[FetchResult[CreateResult]]

  def createOrUpdateProduct(
      id: Option[ProductId],
      p: NewBikeProduct
  ): F[FetchResult[CreateResult]]

  def createOrUpdateComponent(
      id: Option[ComponentId],
      c: NewComponent
  ): F[FetchResult[CreateResult]]
}
