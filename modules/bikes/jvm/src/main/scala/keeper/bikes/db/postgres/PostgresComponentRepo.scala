package keeper.bikes.db.postgres

import java.time.Instant

import cats.data.{NonEmptyList, NonEmptySet}
import cats.effect.*
import cats.syntax.all.*

import keeper.bikes.SimpleQuery
import keeper.bikes.data.{ComponentType, ComponentWithProduct, NewComponent}
import keeper.bikes.db.ComponentRepository
import keeper.bikes.model.BikeServiceError
import keeper.common.Distance
import keeper.core.ComponentId

import skunk.Session
import skunk.data.Completion

final class PostgresComponentRepo[F[_]: Sync](session: Resource[F, Session[F]])
    extends ComponentRepository[F] {

  def storeComponent(c: NewComponent): F[ComponentId] =
    session.use(s =>
      s.transaction
        .use(_ => s.unique(ComponentSql.insert)(c))
    )

  def updateComponent(id: ComponentId, c: NewComponent): F[Boolean] =
    session
      .use(s => s.transaction.use(_ => s.execute(ComponentSql.update)(id -> c)))
      .map {
        case Completion.Update(count) => count > 0
        case _                        => false
      }

  def removeComponents(ids: NonEmptySet[ComponentId], date: Instant): F[Unit] =
    session.use { s =>
      s.execute(ComponentSql.updateRemovedAt(ids.toList))(ids -> date)
    }.void

  def search(query: SimpleQuery): F[List[ComponentWithProduct]] =
    if (query.text.trim.isEmpty)
      session.use(_.execute(ComponentSql.searchAll)(query.page))
    else session.use(_.execute(ComponentSql.searchText)(query))

  def checkTypes(
      data: Seq[(ComponentId, ComponentType)]
  ): F[Option[BikeServiceError]] =
    val tlist = data.toList
    if (tlist.isEmpty) Option.empty.pure[F]
    else
      session
        .use(_.execute(ComponentSql.queryInvalidTypes(tlist))(tlist))
        .map(NonEmptyList.fromList)
        .map(_.map(BikeServiceError.ComponentTypeMismatch.apply))

  def queryTypes(ids: Set[ComponentId]): F[Map[ComponentId, ComponentType]] =
    val idList = ids.toList
    if (idList.isEmpty) Map.empty.pure[F]
    else
      session
        .use(_.execute(ComponentSql.queryComponentTypes(idList))(idList))
        .map(_.toMap)

  def findByIdWithProduct(id: ComponentId, at: Instant): F[Option[ComponentWithProduct]] =
    session.use(_.option(ComponentSql.findByIdWithProduct)(id -> at))

  def findAllAt(at: Instant): F[List[ComponentWithProduct]] =
    session.use(_.execute(ComponentSql.findAllAt)(at))

  def findAllByType(
      types: NonEmptyList[ComponentType],
      at: Instant
  ): F[List[ComponentWithProduct]] =
    session.use(_.execute(ComponentSql.findAllByTypeAt(types.size))(at -> types.toList))

  def findInitialTotals(includes: List[ComponentId]): F[List[(ComponentId, Distance)]] = {
    val f = ComponentSql.findInitialTotals(includes)
    val q = f.fragment.query(Codecs.componentId ~ Codecs.distance)
    session.use(s => s.execute(q)(f.argument))
  }
}
