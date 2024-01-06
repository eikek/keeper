package keeper.bikes.db

import keeper.bikes.SimpleQuery
import keeper.bikes.data.*

trait ProductRepository[F[_]] {

  def storeProduct(p: NewBikeProduct): F[ProductId]
  def updateProduct(id: ProductId, p: NewBikeProduct): F[Boolean]
  def search(productQuery: SimpleQuery): F[List[ProductWithBrand]]
}
