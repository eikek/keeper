package keeper.client

import java.time.Instant

import scala.concurrent.duration.Duration

import cats.effect.*
import cats.syntax.all.*
import fs2.io.net.Network

import keeper.bikes.Page
import keeper.bikes.SimpleQuery
import keeper.bikes.data.*
import keeper.bikes.model._
import keeper.client.data.*
import keeper.core.ComponentId

import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.headers.{Accept, MediaRangeAndQValue}
import org.http4s.{ProductId as _, *}

final class DefaultKeeperClient[F[_]: Async](client: Client[F], baseUrl: Uri)
    extends MoreClientDsl[F]
    with KeeperClient[F] {
  private[this] val logger = scribe.cats.effect[F]

  def getStravaConnectState: F[FetchResult[StravaConnectState]] =
    expect[StravaConnectState](Method.GET(baseUrl / "strava"))

  def getDeviceTotals(at: Option[Instant]): F[FetchResult[List[BikeTotal]]] =
    val url = (baseUrl / "bike" / "distances")
      .withOptionQueryParam("at", at)
    expect[List[BikeTotal]](Method.GET(url))

  def getServices(until: Option[Instant], page: Page): F[FetchResult[List[BikeService]]] =
    val url = (baseUrl / "bike" / "service")
      .withQueryParam("limit", page.limit)
      .withQueryParam("offset", page.offset)
      .withQueryParam("at", until)
    expectOr[List[BikeService]](Method.GET(url)).map(_.map(_.getOrElse(Nil)))

  def getServiceDetails(mask: ServiceSearchMask): F[FetchResult[List[ServiceDetail]]] =
    val url = baseUrl / "bike" / "service"
    expect[List[ServiceDetail]](Method.POST(mask, url))

  def previewService(s: BikeService): F[FetchResult[BikeBuilds]] =
    val url = baseUrl / "bike" / "service" / "preview"
    expect[BikeBuilds](Method.POST(s, url))

  def submitService(s: BikeService): F[FetchResult[BikeBuilds]] =
    val url = baseUrl / "bike" / "service" / "submit"
    expect[BikeBuilds](Method.POST(s, url))

  def getCurrentBikes: F[FetchResult[BikeBuilds]] =
    val url = baseUrl / "bike"
    expect[BikeBuilds](Method.GET(url))

  def getBikesAt(time: Instant): F[FetchResult[BikeBuilds]] =
    val url = (baseUrl / "bike")
      .withQueryParam("at", time)
    expect[BikeBuilds](Method.GET(url))

  def searchBrands(q: Option[String]): F[FetchResult[List[Brand]]] =
    val url = (baseUrl / "brand")
      .withOptionQueryParam("q", q.filter(_.nonEmpty))
    expectOr[List[Brand]](Method.GET(url)).map(_.map(_.getOrElse(Nil)))

  def searchProducts(q: SimpleQuery): F[FetchResult[List[ProductWithBrand]]] =
    val url = (baseUrl / "product")
      .withOptionQueryParam("q", Option(q.text).filter(_.nonEmpty))
      .withQueryParam("limit", q.page.limit)
      .withQueryParam("offset", q.page.offset)
    expectOr[List[ProductWithBrand]](Method.GET(url)).map(_.map(_.getOrElse(Nil)))

  def searchComponents(q: SimpleQuery): F[FetchResult[List[ComponentWithProduct]]] =
    val url = (baseUrl / "component")
      .withOptionQueryParam("q", Option(q.text).filter(_.nonEmpty))
      .withQueryParam("limit", q.page.limit)
      .withQueryParam("offset", q.page.offset)
    expectOr[List[ComponentWithProduct]](Method.GET(url)).map(_.map(_.getOrElse(Nil)))

  def getComponentsAt(time: Instant): F[FetchResult[List[ComponentWithDevice]]] =
    val url = (baseUrl / "component" / "all")
      .withQueryParam("at", time)
    expectOr[List[ComponentWithDevice]](Method.GET(url)).map(_.map(_.getOrElse(Nil)))

  def createOrUpdateBrand(
      id: Option[BrandId],
      brand: NewBrand
  ): F[FetchResult[CreateResult]] =
    val base = baseUrl / "brand"
    id match
      case Some(bid) =>
        logger.debug(s"Update brand: $brand") >>
          expect[CreateResult](Method.PUT(brand, base / bid))

      case None =>
        logger.debug(s"Create new brand: $brand") >>
          expect[CreateResult](Method.POST(brand, base))

  def createOrUpdateProduct(
      id: Option[ProductId],
      p: NewBikeProduct
  ): F[FetchResult[CreateResult]] =
    val base = baseUrl / "product"
    id match
      case Some(pid) =>
        logger.debug(s"Update product: $p") *>
          expect[CreateResult](Method.PUT(p, base / pid))

      case None =>
        logger.debug(s"Create product: $p") *>
          expect[CreateResult](Method.POST(p, base))

  def createOrUpdateComponent(
      id: Option[ComponentId],
      c: NewComponent
  ): F[FetchResult[CreateResult]] =
    val base = baseUrl / "component"
    id match
      case Some(cid) =>
        logger.debug(s"Update component: $c") *>
          expect[CreateResult](Method.PUT(c, base / cid))

      case None =>
        logger.debug(s"Create component: $c") *>
          expect[CreateResult](Method.POST(c, base))

  private def expectOr[A](
      req: Request[F]
  )(using d: EntityDecoder[F, A]): F[FetchResult[Option[A]]] = {
    val r = if (d.consumes.nonEmpty) {
      val m = d.consumes.toList
      req.addHeader(
        Accept(MediaRangeAndQValue(m.head), m.tail.map(MediaRangeAndQValue(_)): _*)
      )
    } else req

    client.run(r).use {
      case Status.Successful(resp) =>
        d.decode(resp, strict = false)
          .leftWiden[Throwable]
          .rethrowT
          .map(a => FetchResult.Success(a.some))
      case failedResponse =>
        failedResponse.status match {
          case Status.NotFound => FetchResult.Success(Option.empty[A]).pure[F]
          case Status.Gone     => FetchResult.Success(Option.empty[A]).pure[F]
          case Status.BadRequest =>
            EntityDecoder[F, RequestFailure]
              .decode(failedResponse, strict = false)
              .leftWiden[Throwable]
              .rethrowT
              .map(FetchResult.RequestFailed.apply)
          case _ =>
            Async[F]
              .raiseError(UnexpectedStatus(failedResponse.status, req.method, req.uri))
        }
    }
  }
  private def expect[A](
      req: Request[F]
  )(using d: EntityDecoder[F, A]): F[FetchResult[A]] =
    expectOr(req).map {
      case FetchResult.Success(Some(a)) => FetchResult.Success(a)
      case FetchResult.Success(None) =>
        FetchResult.RequestFailed(RequestFailure("Request failed", Nil))
      case r => r.asInstanceOf[FetchResult[A]]
    }
}

object DefaultKeeperClient {
  def apply[F[_]: Network: Async](
      baseUrl: Uri,
      httpTimeout: Duration
  ): Resource[F, KeeperClient[F]] =
    EmberClientBuilder
      .default[F]
      .withTimeout(httpTimeout)
      .build
      .map(new DefaultKeeperClient[F](_, baseUrl))
}
