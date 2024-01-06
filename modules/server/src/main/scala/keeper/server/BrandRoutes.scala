package keeper.server

import cats.effect.Sync
import cats.syntax.all.*

import keeper.bikes.data.*
import keeper.bikes.db.BrandRepository
import keeper.client.data.*
import keeper.server.util.*

import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

final class BrandRoutes[F[_]: Sync](repo: BrandRepository[F])
    extends Http4sDsl[F]
    with MoreHttp4sDsl[F] {

  def routes: HttpRoutes[F] = HttpRoutes.of {
    case req @ POST -> Root =>
      for {
        brand <- req.as[NewBrand]
        id <- repo.storeBrand(brand)
        resp <- Ok(CreateResult(id.asInt))
      } yield resp

    case req @ PUT -> Root / BrandIdVar(id) =>
      for {
        brand <- req.as[NewBrand]
        ok <- repo.updateBrand(id, brand)
        resp <- Option
          .when(ok)(CreateResult(id.asInt.toLong))
          .orNotFound(s"No brand with id $id")
      } yield resp

    case GET -> Root :? StringQueryParam(q) =>
      Ok(repo.findBrands(q.map(_ + "%").getOrElse("%"), 50).compile.toList)
  }
}
