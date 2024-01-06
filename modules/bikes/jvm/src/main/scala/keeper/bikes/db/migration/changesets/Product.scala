package keeper.bikes.db.migration.changesets

import keeper.bikes.db.migration.ChangeSet

import skunk.implicits.*

object Product {

  val brand: ChangeSet = ChangeSet.create("create brand table")(
    sql"""
       create table "brand"(
         "id" serial not null primary key,
         "name" varchar not null,
         "description" text,
         "created_at" timestamptz not null default now(),
         constraint "brand_name_uniq" unique("name")
       )""",
    sql"""
       insert into "brand" (name)
       values
          ('Beast'),
          ('Brooks'),
          ('Campagnolo'),
          ('Cane Creek'),
          ('Challenge'),
          ('Continental'),
          ('CrankBrothers'),
          ('Cube'),
          ('DT Swiss'),
          ('Easton'),
          ('Enve'),
          ('Ergon'),
          ('Evoc'),
          ('Favero'),
          ('Fizik'),
          ('FSA'),
          ('Fulcrum'),
          ('Garmin'),
          ('Hope'),
          ('Jagwire'),
          ('K-Edge'),
          ('KCNC'),
          ('Lezyne'),
          ('Lightweight'),
          ('Lizard Skins'),
          ('Look'),
          ('Magura'),
          ('Mavic'),
          ('Newmen'),
          ('Panaracer'),
          ('Pirelli'),
          ('Quaxar'),
          ('Ritchey'),
          ('Schwalbe'),
          ('Selle Italia'),
          ('Shimano'),
          ('SKS'),
          ('SRAM'),
          ('SwissSide'),
          ('SwissStop'),
          ('Syntace'),
          ('Tektro'),
          ('Time'),
          ('TRP'),
          ('Truvativ'),
          ('Tubolito'),
          ('Tune'),
          ('Vittoria'),
          ('Whisky'),
          ('XLC'),
          ('YBN'),
          ('Zipp'),
          ('Hilite'),
          ('Norwid'),
          ('Ribble'),
          ('Colnago'),
          ('Factor'),
          ('Canyon')
       """
  )

  val product: ChangeSet = ChangeSet.create("create product table")(
    sql"""
       create table "product"(
         "id" bigserial not null primary key,
         "brand_id" int not null,
         "name" varchar not null,
         "type_id" int not null,
         "description" text,
         "weight" double precision,
         "created_at" timestamptz not null default now(),
         constraint "product_brand_id_fk"
            foreign key ("brand_id")
               references "brand"("id")
               on delete cascade,
         constraint "product_type_id_fk"
            foreign key ("type_id")
               references "component_type"("id")
               on delete cascade,
         constraint "product_brand_name_type_uniq" unique("brand_id", "type_id", "name")
       )""",
    sql"""
        create index "product_name_idx" on "product"("name")
       """,
    sql"""
       create materialized view "product_brand_view" as
         select
           ct.id as ct_id, ct.name as ct_name,
           b.id as b_id, b.name as b_name, b.description as b_description, b.created_at as b_created_at,
           p.id as p_id, p.brand_id as p_brand_id, p.name as p_name, p.type_id as p_type_id,
           p.description as p_description, p.weight as p_weight, p.created_at as p_created_at
         from "product" p
         inner join "component_type" ct on ct.id = p.type_id
         inner join "brand" b on b.id = p.brand_id
       """,
    sql"""
       create unique index "product_brand_view_id_idx" on "product_brand_view"("ct_id", "b_id", "p_id")
       """,
    sql"""
       create index "product_brand_view_search_idx" on "product_brand_view"
         using gin (to_tsvector('english', ct_name || ' ' || b_name || ' ' || p_name))
       """
  )
}
