package keeper.bikes.db.postgres

import keeper.bikes.data.*
import keeper.bikes.db.postgres.Codecs as c

import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

object BrandSql {

  val insert: Query[NewBrand, BrandId] =
    sql"""
       insert into "brand" (name, description)
       values ($varchar, ${text.opt})
       returning "id"
       """
      .query(int4)
      .contrato[NewBrand]
      .map(BrandId(_))

  val update: Command[(BrandId, NewBrand)] =
    sql"""
       update "brand"
       set "name" = $varchar,
           "description" = ${text.opt}
       where id = ${c.brandId}
       """.command
      .contramap[(BrandId, NewBrand)] { case (id, b) =>
        b.name *: b.description *: id *: EmptyTuple
      }

  val brandCols = sql"""b.id, b.name, b.description, b.created_at"""

  val findById: Query[BrandId, Brand] =
    sql"""
       select $brandCols
       from "brand" b
       where b.id = ${c.brandId}
       """
      .query(c.brand)

  val searchAll: Query[Void, Brand] =
    sql"""
       select $brandCols
       from "brand" b
       order by b.name asc
       """
      .query(c.brand)

  val searchLike: Query[String, Brand] =
    sql"""
       select $brandCols
       from "brand" b where lower(b.name) like $varchar
       order by b.name asc
       """
      .query(c.brand)
      .contramap(_.toLowerCase)
}
