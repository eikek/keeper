package keeper.bikes.db

import fs2.Stream

import keeper.bikes.data.{Brand, BrandId, NewBrand}

trait BrandRepository[F[_]] {

  def storeBrand(b: NewBrand): F[BrandId]

  def updateBrand(id: BrandId, b: NewBrand): F[Boolean]

  def findBrands(nameLike: String, chunkSize: Int): Stream[F, Brand]

  def findById(id: BrandId): F[Option[Brand]]
}
