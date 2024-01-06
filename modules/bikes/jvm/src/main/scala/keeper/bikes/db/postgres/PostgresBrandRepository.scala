package keeper.bikes.db.postgres
import cats.effect.{Resource, Sync}
import cats.syntax.all.*
import fs2.Stream

import keeper.bikes.data.{Brand, BrandId, NewBrand}
import keeper.bikes.db.BrandRepository

import skunk.Session
import skunk.data.Completion

final class PostgresBrandRepository[F[_]: Sync](session: Resource[F, Session[F]])
    extends BrandRepository[F] {

  override def storeBrand(b: NewBrand): F[BrandId] =
    session.use(_.unique(BrandSql.insert)(b))

  override def updateBrand(id: BrandId, b: NewBrand): F[Boolean] =
    session.use(s => s.transaction.use(_ => s.execute(BrandSql.update)(id -> b))).map {
      case Completion.Update(count) => count > 0
      case _                        => false
    }

  override def findBrands(nameLike: String, chunkSize: Int): Stream[F, Brand] =
    for {
      s <- Stream.resource(session)
      ps <- Stream.eval {
        if (nameLike.isEmpty)
          s.prepare(BrandSql.searchAll.contramap[String](_ => skunk.Void))
        else s.prepare(BrandSql.searchLike)
      }
      r <- ps.stream(nameLike, chunkSize)
    } yield r

  override def findById(id: BrandId): F[Option[Brand]] =
    session.use(_.option(BrandSql.findById)(id))
}
