package keeper.bikes.db.postgres

import keeper.bikes.data.*
import keeper.bikes.db.postgres.Codecs as c
import keeper.bikes.{Page, SimpleQuery}

import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

object ProductSql {
  private val selectTypeId =
    sql"""(select id from component_type where name = ${c.componentType})"""

  val insert: Query[NewBikeProduct, ProductId] =
    sql"""
       insert into "product" (brand_id, name, type_id, description, weight)
       values (${c.brandId}, $varchar, $selectTypeId, ${text.opt}, ${c.weight.opt})
       returning id
       """
      .query(int8)
      .contramap[NewBikeProduct](e =>
        e.brandId *: e.name *: e.productType *: e.description *: e.weight *: EmptyTuple
      )
      .map(ProductId(_))

  val update: Command[(ProductId, NewBikeProduct)] =
    sql"""
       update "product"
         set "brand_id" = ${c.brandId},
             "type_id" = $selectTypeId,
             "name" = $varchar,
             "description" = ${text.opt},
             "weight" = ${c.weight.opt}
       where "id" = ${c.productId}
       """.command
      .contramap[(ProductId, NewBikeProduct)] { case (id, p) =>
        p.brandId *: p.productType *: p.name *: p.description *: p.weight *: id *: EmptyTuple
      }

  val refreshView: Command[Void] =
    sql"""refresh materialized view concurrently "product_brand_view";""".command

  val productWithBrandCols =
    sql"""
       p_id, p_brand_id, ct_name, p_name, p_description, p_weight, p_created_at,
       b_id, b_name, b_description, b_created_at
       """

  val searchText: Query[SimpleQuery, ProductWithBrand] =
    sql"""
       select
          $productWithBrandCols
       from product_brand_view
       where
        to_tsvector('english', ct_name || ' ' || b_name || ' ' || p_name) @@ to_tsquery('english', $varchar)
       limit $int4
       offset $int8
       """
      .query(c.productWithBrand)
      .contramap[SimpleQuery](q =>
        val words =
          q.text.split("\\s+").map(_.trim.toLowerCase).map(w => s"$w:*").mkString(" & ")
        words *: q.page.limit *: q.page.offset *: EmptyTuple
      )

  val searchAll: Query[Page, ProductWithBrand] =
    sql"""
     select
         $productWithBrandCols
     from product_brand_view
     limit $int4
     offset $int8
     """
      .query(c.productWithBrand)
      .contramap[Page](page => page.limit *: page.offset *: EmptyTuple)

}
