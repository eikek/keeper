package keeper.bikes.db.postgres

import cats.effect.{Resource, Sync}
import cats.syntax.all.*

import keeper.bikes.SimpleQuery
import keeper.bikes.data.*
import keeper.bikes.db.ProductRepository

import skunk.Session
import skunk.data.Completion

final class PostgresProductRepository[F[_]: Sync](session: Resource[F, Session[F]])
    extends ProductRepository[F] {
  private val logger = scribe.cats.effect[F]

  override def storeProduct(p: NewBikeProduct): F[ProductId] =
    session.use(s =>
      s.transaction
        .use(_ => s.unique(ProductSql.insert)(p))
        .flatTap(_ => s.execute(ProductSql.refreshView))
    )

  override def updateProduct(id: ProductId, p: NewBikeProduct): F[Boolean] =
    session
      .use(s =>
        s.transaction
          .use(_ => s.execute(ProductSql.update)(id -> p))
          .flatTap(_ => s.execute(ProductSql.refreshView))
      )
      .flatMap {
        case Completion.Update(count) => (count > 0).pure[F]
        case c =>
          logger.warn(s"Updating bike product failed: $c").as(false)
      }

  override def search(query: SimpleQuery): F[List[ProductWithBrand]] =
    if (query.text.trim.isEmpty) session.use(_.execute(ProductSql.searchAll)(query.page))
    else session.use(_.execute(ProductSql.searchText)(query))
}
