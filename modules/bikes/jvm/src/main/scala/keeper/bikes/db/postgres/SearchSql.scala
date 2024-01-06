package keeper.bikes.db.postgres

import cats.data.NonEmptyList
import cats.syntax.all.*

import keeper.bikes.db.postgres.Codecs as c
import keeper.bikes.model.{ServiceDetail, ServiceSearchMask}

import skunk.*
import skunk.codec.all as a
import skunk.implicits.*

object SearchSql {

  private val bikes = c.jsonb[Set[ServiceDetail.AffectedBike]]
  private val entries = c.jsonb[List[ServiceDetail.ServiceEntry]]

  val resultCodec: Codec[ServiceDetail] =
    (c.maintenanceId *: a.varchar *: a.text.opt *: c.instant *: c.serviceEventNameSet *: bikes *: entries *: ConfigurationCache.totalsCodec)
      .to[ServiceDetail]

  def searchQueryFragment(mask: ServiceSearchMask): AppliedFragment = {
    val start = sql"""with"""
    val ma = maintArr(mask)
    val me = maintEvs
    val mea = maintEvsAgg
    val maa = maintAgg

    val rest = sql"""
      select ma.id, ma.name, ma.description, ma.date, ma.service_names, ma.devices, coalesce(me.events, '[]'::jsonb), cache.totals_tracker
      from maint_agg ma
      inner join configuration_cache cache on cache.maintenance_id = ma.id
      left join maint_evs_agg me on me.maint_id = ma.id
      order by ma.date asc
    """
    start(Void) |+| ma |+| me(Void) |+| mea(Void) |+| maa(Void) |+| rest(Void)
  }

  private def maintAgg =
    sql"""
       maint_agg(id, name, description, date, service_names, devices)
          as (
            select
            mm.id,
            mm.name,
            mm.description,
            mm.date,
            mm.service_names,
            coalesce(jsonb_agg(
              case when d.id is null then null::jsonb
               else jsonb_build_object('id', d.id, 'name', d.name, 'total', dt.totals)
              end
            ) filter (where d.id is not null), '[]'::jsonb) as devices
           from maint_arr mm
           left join device d on d.id = any(mm.device_ids)
           left join maintenance_device_total dt on dt.device_id = d.id and dt.maintenance_id = mm.id
           group by mm.id, mm.name, mm.description, mm.date, mm.service_names
         )
       """

  private def maintEvsAgg =
    sql"""
       maint_evs_agg (maint_id, events)
          as (
            select me.maint_id,
              jsonb_agg(
                jsonb_build_object(
                   'index', me.index,
                   'action', me.action_name,
                   'serviceName', me.service_name,
                   'bike',
                      case when me.dev_id is null then null::jsonb
                      else jsonb_build_object(
                             'id', me.dev_id,
                             'name', me.dev_name
                           )
                      end,
                   'component',
                      case when me.comp_id is null then null::jsonb
                      else jsonb_build_object(
                            'id', me.comp_id,
                            'name', me.comp_name,
                            'type', me.comp_type,
                            'initial', me.comp_initial
                           )
                      end,
                    'sub',
                       case when sub_id is null then null::jsonb
                       else jsonb_build_object(
                              'id', me.sub_id,
                             'name', me.sub_name,
                             'type', me.sub_type,
                             'initial', me.sub_initial
                            )
                       end
                 )
              ) as events
            from maint_evs me
            group by me.maint_id
          ),
       """

  private def maintEvs =
    sql"""
       maint_evs(maint_id, index, action_name, service_name, dev_id, dev_name, comp_id, comp_name, comp_type, comp_initial, sub_id, sub_name, sub_type, sub_initial)
       as (
         select
           me.maintenance_id,
           me.index,
           a.name as action_name,
           sen.name as service_name,
           me.device_id as dev_id,
           d.name as dev_name,
           me.component_id,
           c1.name as comp_name,
           v1.ct_name as comp_type,
           c1.initial_total as comp_initial,
           me.sub_component_id as sub_id,
           c2.name as sub_name,
           v2.ct_name as sub_type,
           c2.initial_total as sub_initial
         from maintenance_event me
         inner join action a on a.id = me.action_id
         inner join service_event se on se.id = me.service_event_id
         inner join service_event_name sen on sen.id = se.name_id
         left join component c1 on c1.id = me.component_id
         left join product_brand_view v1 on v1.p_id = c1.product_id
         left join component c2 on c2.id = me.sub_component_id
         left join product_brand_view v2 on v2.p_id = c2.product_id
         left join device d on d.id = me.device_id
         order by me.maintenance_id, dev_id, comp_type, sub_type, a.id desc
        ),
        """

  private def maintArr(m: ServiceSearchMask): AppliedFragment = {
    val base = sql"""
       maint_arr(id, name, description, date, actions, device_ids, service_names)
       as (
          select
            m.id, m.name, m.description, m.date,
            array_remove(array_agg(distinct a.name), null) as actions,
            array_remove(array_agg(distinct me.device_id), null) as device_ids,
            array_remove(array_agg(distinct sen.name), null) as service_names
          from maintenance m
          left join maintenance_event me on me.maintenance_id = m.id
          left join action a on a.id = me.action_id
          left join service_event se on se.maintenance_id = m.id
          inner join service_event_name sen on sen.id = se.name_id
          """

    val filter = whereClause(m)

    val rest = sql"""
       group by (m.id, m.name, m.description, m.date, m.created_at)
       order by m.date desc
     ),"""

    base(Void) |+| filter |+| rest(Void)
  }

  private def whereClause(m: ServiceSearchMask): AppliedFragment =
    val filter =
      List(
        m.untilDate.map(sql"m.date < ${c.instant}"),
        NonEmptyList
          .fromList(m.actions.toList)
          .map(acts =>
            sql"a.name in ${c.actionName.list(acts.size).values}".apply(acts.toList)
          ),
        NonEmptyList
          .fromList(m.serviceEventNames.toList)
          .map(sns =>
            sql"sen.name in ${c.serviceEventName.list(sns.size).values}".apply(sns.toList)
          ),
        NonEmptyList
          .fromList(m.affectedBikes.toList)
          .map(b =>
            sql"me.device_id in ${c.deviceId.list(b.size).values}".apply(b.toList)
          ),
        NonEmptyList
          .fromList(m.affectedComponents.toList)
          .map(cs =>
            val enc = c.componentId.list(cs.size).values
            sql"(me.component_id in $enc OR me.sub_component_id in $enc)".apply(
              cs.toList -> cs.toList
            )
          )
      ).flatten

    if (filter.isEmpty) AppliedFragment.empty
    else filter.foldSmash(void" WHERE ", void" AND ", AppliedFragment.empty)
}
