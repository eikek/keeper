package keeper.server

import cats.effect.Async
import cats.syntax.all.*

import keeper.bikes.data.{NewBikeProduct, ProductWithBrand}
import keeper.bikes.db.ProductRepository
import keeper.client.data.*
import keeper.server.util.{MoreHttp4sDsl, ProductIdVar, ProductQueryParam}

import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

final class ProductRoutes[F[_]: Async](repo: ProductRepository[F])
    extends Http4sDsl[F]
    with MoreHttp4sDsl[F] {

  def routes: HttpRoutes[F] = HttpRoutes.of {
    case req @ POST -> Root =>
      for {
        p <- req.as[NewBikeProduct]
        id <- repo.storeProduct(p)
        resp <- Ok(CreateResult(id.asLong))
      } yield resp

    case req @ PUT -> Root / ProductIdVar(id) =>
      for {
        p <- req.as[NewBikeProduct]
        ok <- repo.updateProduct(id, p)
        resp <- Option
          .when(ok)(CreateResult(id.asLong))
          .orNotFound(s"No product with id $id")
      } yield resp

    case GET -> Root :? ProductQueryParam(query) =>
      query
        .map(repo.search)
        .map(Ok(_))
        .orBadRequest
  }
}
