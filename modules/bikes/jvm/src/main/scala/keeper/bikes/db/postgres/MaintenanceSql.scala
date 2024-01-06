package keeper.bikes.db.postgres

import java.time.Instant

import cats.syntax.all.*

import keeper.bikes.Page
import keeper.bikes.data.*
import keeper.bikes.db.postgres.Codecs as c
import keeper.bikes.event.ServiceEvent
import keeper.bikes.model.{BikeService, BikeTotal}
import keeper.core.{Maintenance as _, *}

import skunk.*
import skunk.codec.all as a
import skunk.implicits.*

object MaintenanceSql {

  val insertNoTotals: Query[BikeService, MaintenanceId] =
    sql"""
       insert into "maintenance" (name, description, date)
       values (${a.varchar}, ${a.text.opt}, ${c.instant})
       returning id
       """
      .query(c.maintenanceId)
      .contramap[BikeService](m => m.name *: m.description *: m.date *: EmptyTuple)

  def insertDeviceTotal(
      totals: List[(MaintenanceId, BikeTotal)]
  ): Command[List[(MaintenanceId, BikeTotal)]] = {
    val codec = (c.maintenanceId *: (c.deviceId *: c.distance).to[BikeTotal]).values
      .list(totals.length)
    sql"""
       insert into maintenance_device_total (maintenance_id, device_id, totals)
       values $codec
       """.command
  }

  val selectTotals: Query[MaintenanceId, (DeviceId, TotalOutput)] =
    sql"""
       select device_id, totals
       from maintenance_device_total m
       where m.id = ${c.maintenanceId}
       """
      .query(c.deviceId *: c.totalOutput)

  val serviceNameId =
    sql"""select id from service_event_name where name = ${c.serviceEventName}"""

  val insertServiceEvent: Query[(MaintenanceId, Int, ServiceEvent), ServiceEventId] =
    sql"""
       insert into service_event (maintenance_id, index, name_id, version, event_data)
       values (${c.maintenanceId}, ${a.int4}, ($serviceNameId), 1, ${c.serviceEventJson})
       returning id
       """
      .query(c.serviceEventId)
      .contramap[(MaintenanceId, Int, ServiceEvent)] { case (mid, idx, ev) =>
        mid *: idx *: ev.eventName *: ev *: EmptyTuple
      }

  val actionIdByName = sql"""select id from action where name = ${c.actionName}"""

  val insertEvent: Query[(ServiceEventId, NewMaintenanceEvent), MaintenanceEventId] =
    sql"""
       insert into maintenance_event (service_event_id, maintenance_id, index, action_id, device_id, component_id, sub_component_id)
       values (${c.serviceEventId}, ${c.maintenanceId}, ${a.int4}, ($actionIdByName), ${c.deviceId.opt}, ${c.componentId.opt}, ${c.componentId.opt})
       returning id
       """
      .query(c.maintenanceEventId)
      .contramap[(ServiceEventId, NewMaintenanceEvent)] { case (id, e) =>
        id *: e.maintenance *: e.index *: e.action *: e.device *: e.component *: e.subComponent *: EmptyTuple
      }

  val maintenanceWithTotalsAfter
      : Query[MaintenanceId, (MaintenanceId, Option[DeviceId], Option[TotalOutput])] =
    sql"""
       select m.id , t.device_id, t.totals
       from maintenance m
       left outer join maintenance_device_total t on t.maintenance_id = m.id
       where m.date > (select date from maintenance where id = ${c.maintenanceId})
       order by m.date asc
       """
      .query(c.maintenanceId *: c.deviceId.opt *: c.totalOutput.opt)

  val maintenanceWithTotalsUpTo
      : Query[Instant, (MaintenanceId, Option[DeviceId], Option[TotalOutput])] =
    sql"""
       select m.id , t.device_id, t.totals
       from maintenance m
       left outer join maintenance_device_total t on t.maintenance_id = m.id
       where m.date < ${c.instant}
       order by m.date asc
       """
      .query(c.maintenanceId *: c.deviceId.opt *: c.totalOutput.opt)

  val maintenanceWithTotalsBetween: Query[
    (Instant, MaintenanceId),
    (MaintenanceId, Option[DeviceId], Option[TotalOutput])
  ] =
    sql"""
       select m.id , t.device_id, t.totals
       from maintenance m
       left outer join maintenance_device_total t on t.maintenance_id = m.id
       where m.date < ${c.instant} and m.date > (select date from maintenance where id = ${c.maintenanceId})
       order by m.date asc
       """
      .query(c.maintenanceId *: c.deviceId.opt *: c.totalOutput.opt)

  val allMaintenanceWithTotals
      : Query[Void, (MaintenanceId, Option[DeviceId], Option[TotalOutput])] =
    sql"""
       select m.id , t.device_id, t.totals
       from maintenance m
       left outer join maintenance_device_total t on t.maintenance_id = m.id
       order by m.date asc
       """
      .query(c.maintenanceId *: c.deviceId.opt *: c.totalOutput.opt)

  private val maintenanceEventCols =
    sql"e.id, e.maintenance_id, e.index, a.name, e.device_id, e.component_id, e.sub_component_id, e.created_at"

  val maintenanceEventsFor: Query[MaintenanceId, MaintenanceEvent] =
    sql"""
       select $maintenanceEventCols
       from maintenance_event e
       inner join action a on a.id = e.action_id
       where e.maintenance_id = ${c.maintenanceId}
       order by e.index asc
       """
      .query(c.maintenanceEvent)

  val clearCacheAfter: Command[Instant] =
    sql"""
       delete from "configuration_cache"
       where maintenance_id in (select id from maintenance where date >= ${c.instant})
       """.command

  private val bikeServiceSelect =
    sql"""
       select m.id, m.name, m.description, m.date, m.created_at
       from maintenance m
       """

  val findAllBikeServices: Query[Void, (MaintenanceId, BikeService)] =
    sql"""
       $bikeServiceSelect
       order by m.date desc
       """
      .query(c.maintenanceId ~ c.bikeServiceMain)

  val findAllBikeServicesPaged: Query[Page, (MaintenanceId, BikeService)] =
    sql"""
     $bikeServiceSelect
     order by m.date desc
     limit ${a.int4}
     offset ${a.int8}
     """
      .query(c.maintenanceId ~ c.bikeServiceMain)
      .contramap[Page](p => p.limit *: p.offset *: EmptyTuple)

  val findAllBikeServicesPagedUntil
      : Query[(Instant, Page), (MaintenanceId, BikeService)] =
    sql"""
       $bikeServiceSelect
       where m.date < ${c.instant}
       order by m.date desc
       limit ${a.int4}
       offset ${a.int8}
       """
      .query(c.maintenanceId ~ c.bikeServiceMain)
      .contramap[(Instant, Page)](t => t._1 *: t._2.limit *: t._2.offset *: EmptyTuple)

  val findServiceEvents: Query[MaintenanceId, ServiceEvent] =
    sql"""
       select event_data
       from service_event
       where maintenance_id = ${c.maintenanceId}
         and version = 1
       order by index asc
       """
      .query(c.serviceEventJson)
}
