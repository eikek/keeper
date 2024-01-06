package keeper.bikes.db.migration.changesets

import keeper.bikes.db.migration.ChangeSet

import skunk.implicits.*

object DeviceComponent {

  val get: ChangeSet = ChangeSet.create("device table")(
    sql"""
       create table "device"(
         "id" bigserial not null primary key,
         "brand_id" int not null,
         "name" varchar not null,
         "description" text,
         "state" varchar not null,
         "added_at" timestamptz not null,
         "removed_at" timestamptz,
         "created_at" timestamptz not null default now(),
         constraint "device_name_uniq" unique("name"),
         constraint "device_brand_id_fk"
           foreign key ("brand_id")
             references "brand"("id")
             on delete cascade
       )
       """,
    sql"""
       create table "component_state"(
         "id" serial not null primary key,
         "name" varchar not null,
         constraint "component_state_name_uniq" unique("name")
       )""",
    sql"""
       INSERT INTO "component_state" ("name")
         VALUES
           ('active'),
           ('inactive'),
           ('gone')
       """,
    sql"""
       create table "component"(
         "id" bigserial not null primary key,
         "product_id" bigint not null,
         "name" varchar not null,
         "description" text,
         "state_id" int not null,
         "added_at" timestamptz not null,
         "removed_at" timestamptz,
         "initial_total" double precision not null,
         "created_at" timestamptz not null default now(),
         constraint "component_name_uniq" unique("name"),
         constraint "component_product_id_fk"
            foreign key ("product_id")
              references "product"("id")
              on delete no action,
         constraint "component_state_id_fk"
            foreign key ("state_id")
              references "component_state"("id")
              on delete no action
       )
       """,
    sql"""
       create index "component_added_at_idx" on "component"("added_at")
       """
  )
}
