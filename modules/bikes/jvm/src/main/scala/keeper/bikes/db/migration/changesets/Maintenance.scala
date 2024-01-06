package keeper.bikes.db.migration.changesets

import keeper.bikes.db.migration.ChangeSet

import skunk.implicits.*

object Maintenance {
  val action: ChangeSet = ChangeSet.create("action table")(
    sql"""
       create table "action"(
         "id" serial not null primary key,
         "name" varchar not null,
         "description" text,
         "created_at" timestamptz not null default now(),
         constraint "action_name_uniq" unique("name")
       )
       """,
    sql"""
       insert into "action" ("name", "description")
       values
         ('add', 'Add component to device or sub-component to component'),
         ('remove', 'Remove a component from a device or a sub-component from a component'),
         ('drop', 'Remove components or bikes from the configuration'),
         ('hotwax', 'Chain waxing using hot wax'),
         ('dripwax', 'Chain waxing using drip wax'),
         ('clean', 'Clean bike or component'),
         ('cease', 'Cease/retire components or bikes'),
         ('patch', 'Patch or repair components')
       """
  )

  val maintenance: ChangeSet = ChangeSet.create("maintenance table")(
    sql"""
       create table "maintenance"(
         "id" bigserial not null primary key,
         "name" varchar not null,
         "description" text,
         "date" timestamptz not null,
         "created_at" timestamptz not null default now()
       )
       """,
    sql"""
       create index "maintenance_date_idx" on "maintenance"("date")
       """,
    sql"""
       create table "maintenance_device_total"(
         "id" bigserial not null primary key,
         "maintenance_id" bigserial not null,
         "device_id" bigserial not null,
         "totals" double precision not null,
         constraint "maintenance_device_total_maintenance_id_fk"
           foreign key ("maintenance_id")
             references "maintenance"("id")
             on delete no action,
         constraint "maintenance_device_total_device_id_fk"
           foreign key ("device_id")
             references "device"("id")
             on delete no action
       )
       """
  )

  val serviceEvent: ChangeSet = ChangeSet.create("service event")(
    sql"""
       create table "service_event_name"(
         "id" serial not null primary key,
         "name" varchar not null,
         "created_at" timestamptz not null default now(),
          constraint "service_event_name_uniq" unique("name")
       )
       """,
    sql"""
       insert into "service_event_name" ("name")
       values
         ('newbike'),
         ('changebike'),
         ('changefrontwheel'),
         ('changerearwheel'),
         ('changefork'),
         ('changetires'),
         ('changebrakepads'),
         ('waxchain'),
         ('ceasecomponent'),
         ('ceasebike'),
         ('patchtube'),
         ('patchtire'),
         ('cleancomponent'),
         ('cleanbike')
       """,
    sql"""
       create table "service_event"(
         "id" bigserial not null primary key,
         "maintenance_id" bigint not null,
         "index" int not null,
         "name_id" int not null,
         "version" int not null,
         "event_data" jsonb not null,
         "created_at" timestamptz not null default now(),
         constraint "service_event_maintenance_id_fk"
            foreign key ("maintenance_id")
               references "maintenance"("id")
               on delete cascade
       )
       """
  )

  val maintenanceEvent: ChangeSet = ChangeSet.create("maintenance event")(
    sql"""
       create table "maintenance_event"(
         "id" bigserial not null primary key,
         "maintenance_id" bigint not null,
         "service_event_id" bigint not null,
         "index" int not null,
         "action_id" bigint not null,
         "device_id" bigint,
         "component_id" bigint,
         "sub_component_id" bigint,
         "created_at" timestamptz not null default now(),
         constraint "maintenance_event_index_uniq"
           unique ("maintenance_id", "index"),
         constraint "maintenance_event_maintenance_id_fk"
           foreign key ("maintenance_id")
             references "maintenance"("id")
             on delete cascade,
         constraint "maintenance_event_service_event_fk"
           foreign key ("service_event_id")
             references "service_event"("id"),
         constraint "maintenance_event_action_id_fk"
           foreign key ("action_id")
             references "action"("id")
             on delete no action,
         constraint "maintenance_event_device_id_fk"
           foreign key ("device_id")
             references "device"("id")
             on delete no action,
         constraint "maintenance_event_component_id_fk"
           foreign key ("component_id")
             references "component"("id")
             on delete no action,
         constraint "maintenance_event_sub_component_id_fk"
           foreign key ("sub_component_id")
             references "component"("id")
             on delete no action
       )
       """,
    sql"""
       create index "maintenance_event_maintenance_id_index_idx"
         on "maintenance_event"("maintenance_id", "index")
       """,
    sql"""
       create index "maintenance_event_action_component_idx"
         on "maintenance_event"("action_id", "component_id")
      """
  )

  val cache: ChangeSet = ChangeSet.create("configuration cache")(
    sql"""
       create table "configuration_cache"(
         "id" bigserial not null primary key,
         "maintenance_id" bigint not null,
         "version" int not null,
         "configuration" jsonb not null,
         "totals_tracker" jsonb not null,
         "created_at" timestamptz not null default now(),
         constraint "configuration_cache_maintenance_id_fk"
           foreign key ("maintenance_id")
             references "maintenance"("id")
             on delete cascade,
         constraint "configuration_cache_maintenance_uniq"
           unique("maintenance_id", "version")
       )
       """
  )
}
