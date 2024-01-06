package keeper.bikes.db

import java.time.Instant

import cats.data.{NonEmptyList, NonEmptySet}

import keeper.bikes.SimpleQuery
import keeper.bikes.data.{ComponentType, ComponentWithProduct, NewComponent}
import keeper.bikes.model.BikeServiceError
import keeper.core.ComponentId

trait ComponentRepository[F[_]] {

  def storeComponent(c: NewComponent): F[ComponentId]
  def updateComponent(id: ComponentId, c: NewComponent): F[Boolean]
  def search(query: SimpleQuery): F[List[ComponentWithProduct]]

  def removeComponents(ids: NonEmptySet[ComponentId], date: Instant): F[Unit]

  def checkTypes(
      data: Seq[(ComponentId, ComponentType)]
  ): F[Option[BikeServiceError]]

  def queryTypes(ids: Set[ComponentId]): F[Map[ComponentId, ComponentType]]

  def findByIdWithProduct(id: ComponentId, at: Instant): F[Option[ComponentWithProduct]]

  def findAllAt(at: Instant): F[List[ComponentWithProduct]]

  def findAllByType(
      types: NonEmptyList[ComponentType],
      at: Instant
  ): F[List[ComponentWithProduct]]
}
