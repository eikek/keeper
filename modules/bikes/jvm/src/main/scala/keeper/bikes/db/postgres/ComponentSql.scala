package keeper.bikes.db.postgres

import java.time.Instant

import cats.data.NonEmptySet
import cats.syntax.all.*

import keeper.bikes.data.*
import keeper.bikes.db.postgres.Codecs as c
import keeper.bikes.model.BikeServiceError
import keeper.bikes.{Page, SimpleQuery}
import keeper.core.ComponentId

import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

object ComponentSql {
  private val selectStateId =
    sql"""(select id from component_state where name = ${c.componentState})"""

  val insert: Query[NewComponent, ComponentId] =
    sql"""
       insert into "component" (product_id, name, description, state_id, added_at, initial_total)
       values (${c.productId}, $varchar, ${text.opt}, $selectStateId, ${c.instant}, ${c.totalOutput})
       returning id
       """
      .query(int8)
      .contramap[NewComponent](e =>
        e.product *: e.name *: e.description *: e.state *: e.addedAt *: e.initialTotal *: EmptyTuple
      )
      .map(ComponentId(_))

  val update: Command[(ComponentId, NewComponent)] =
    sql"""
       update "component"
       set
          product_id = ${c.productId},
          name = $varchar,
          description = ${text.opt},
          state_id = $selectStateId,
          added_at = ${c.instant},
          initial_total = ${c.totalOutput}
       where id = ${c.componentId}
       """.command
      .contramap[(ComponentId, NewComponent)] { case (id, nc) =>
        nc.product *: nc.name *: nc.description *: nc.state *: nc.addedAt *: nc.initialTotal *: id *: EmptyTuple
      }

  def updateRemovedAt(list: List[ComponentId]) =
    sql"""
       update "component"
       set removed_at = ${c.instant}
       where id in ${c.componentId.list(list.size).values}
         and removed_at is null
       """.command
      .contramap[(NonEmptySet[ComponentId], Instant)] { case (ids, time) =>
        time *: ids.toList *: EmptyTuple
      }

  private val componentColumns =
    sql"""c.id, c.product_id, c.name, c.description, s.name, c.added_at, c.removed_at, c.initial_total, c.created_at"""

  private val componentWithProductSelect =
    sql"""
       select
          ${ProductSql.productWithBrandCols},
          $componentColumns
       from product_brand_view v
       inner join component c on c.product_id = p_id
       inner join component_state s on s.id = c.state_id
       """

  val searchText: Query[SimpleQuery, ComponentWithProduct] =
    sql"""
       $componentWithProductSelect
       where
        to_tsvector('english', c.name || ' ' || ct_name || ' ' || b_name || ' ' || p_name) @@ to_tsquery('english', $varchar)
       limit $int4
       offset $int8
       """
      .query(c.componentWithProduct)
      .contramap[SimpleQuery](q =>
        val words =
          q.text.split("\\s+").map(_.trim.toLowerCase).map(w => s"$w:*").mkString(" & ")
        words *: q.page.limit *: q.page.offset *: EmptyTuple
      )

  val searchAll: Query[Page, ComponentWithProduct] =
    sql"""
     $componentWithProductSelect
     order by ct_name, c.name
     limit $int4
     offset $int8
     """
      .query(c.componentWithProduct)
      .contramap[Page](page => page.limit *: page.offset *: EmptyTuple)

  private val whereAt =
    sql"""c.added_at <= ${c.instant}
          and (c.removed_at is null or c.removed_at > ${c.instant})"""

  val findById: Query[ComponentId, Component] =
    sql"""
       $componentColumns
       where c.id = ${c.componentId}
       """
      .query(c.component)

  val findByIdWithProduct: Query[(ComponentId, Instant), ComponentWithProduct] =
    sql"""
       $componentWithProductSelect
       WHERE c.id = ${c.componentId}
         and $whereAt
       """
      .query(c.componentWithProduct)
      .contramap[(ComponentId, Instant)](t => (t._1, (t._2, t._2)))

  def queryInvalidTypes(
      list: List[(ComponentId, ComponentType)]
  ): Query[List[(ComponentId, ComponentType)], BikeServiceError.InvalidType] =
    val pairEnc = (c.componentId ~ c.componentType).values.list(list.size)
    sql"""
       with data(cid, tname) as (values $pairEnc)
       select c.id, d.tname, v.ct_name
       from product_brand_view v
       inner join component c on c.product_id = v.p_id
       inner join data d on d.cid = c.id
       where d.tname <> v.ct_name
       """
      .query(c.bikeServiceErrorInvalidType)

  def queryComponentTypes(
      ids: List[ComponentId]
  ) =
    val idsEnc = c.componentId.values.list(ids.size)
    sql"""
       with data(cid) as (values $idsEnc)
       select c.id, v.ct_name
       from product_brand_view v
       inner join component c on c.product_id = v.p_id
       inner join data d on d.cid = c.id
       """
      .query(c.componentId ~ c.componentType)

  val findAllAt: Query[Instant, ComponentWithProduct] =
    sql"""
       $componentWithProductSelect
       where $whereAt
       order by ct_name, c.name
       """
      .query(c.componentWithProduct)
      .contramap[Instant](i => i *: i *: EmptyTuple)

  def findAllByTypeAt(
      n: Int
  ): Query[(Instant, List[ComponentType]), ComponentWithProduct] =
    sql"""
      $componentWithProductSelect
      where $whereAt
        and ct_name in ${c.componentType.list(n).values}
      order by ct_name, c.name
      """
      .query(c.componentWithProduct)
      .contramap[(Instant, List[ComponentType])](t => ((t._1, t._1), t._2))

  def findInitialTotals(at: Instant, includes: List[ComponentId]): AppliedFragment = {
    val query =
      sql"""
        select id, initial_total
        from component c
        where $whereAt AND c.initial_total > 0
        """

    val where = includes match
      case Nil => AppliedFragment.empty
      case ids => sql" AND c.id in ${c.componentId.list(ids).values}".apply(ids)

    query(at -> at) |+| where
  }
}
