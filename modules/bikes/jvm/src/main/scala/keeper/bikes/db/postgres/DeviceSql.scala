package keeper.bikes.db.postgres

import java.time.Instant

import cats.data.NonEmptySet
import cats.syntax.all.*

import keeper.bikes.data.{Device, DeviceWithBrand, NewDevice}
import keeper.bikes.db.postgres.Codecs as c
import keeper.core.DeviceId

import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

object DeviceSql {

  val insert: Query[NewDevice, DeviceId] =
    sql"""
       insert into device (brand_id, name, description, state, added_at)
       values (${c.brandId}, $varchar, ${text.opt}, ${c.componentState}, ${c.instant})
       returning id
       """
      .query(c.deviceId)
      .contrato[NewDevice]

  val deviceCols =
    sql"""d.id, d.brand_id, d.name, d.description, d.state, d.added_at, d.removed_at, d.created_at"""

  private val whereAt =
    sql"""d.added_at <= ${c.instant}
          and (d.removed_at is null OR d.removed_at > ${c.instant})"""

  val findById: Query[DeviceId, Device] =
    sql"""
       select
       from device
       where id = ${c.deviceId}
       """
      .query(c.device)

  val findByIdWithBrand: Query[(DeviceId, Instant), DeviceWithBrand] =
    sql"""
       select $deviceCols, ${BrandSql.brandCols}
       from device d
       inner join brand b on b.id = d.brand_id
       where d.id = ${c.deviceId}
         and $whereAt
       """
      .query(c.deviceWithBrand)
      .contramap[(DeviceId, Instant)](t => (t._1, (t._2, t._2)))

  val findAll: Query[Instant, DeviceWithBrand] =
    sql"""
         select $deviceCols, ${BrandSql.brandCols}
         from device d
         inner join brand b on b.id = d.brand_id
         where $whereAt
         order by d.added_at desc, d.name asc
         """
      .query(c.deviceWithBrand)
      .contramap[Instant](i => i *: i *: EmptyTuple)

  def updateRemovedAt(list: List[DeviceId]) =
    sql"""
     update "device"
     set removed_at = ${c.instant}
     where id in ${c.deviceId.list(list.size).values}
       and removed_at is null
     """.command
      .contramap[(NonEmptySet[DeviceId], Instant)] { case (ids, time) =>
        time *: ids.toList *: EmptyTuple
      }
}
