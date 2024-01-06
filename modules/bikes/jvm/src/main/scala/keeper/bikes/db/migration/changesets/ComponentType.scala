package keeper.bikes.db.migration.changesets

import keeper.bikes.db.migration.ChangeSet

import skunk.implicits.*

object ComponentType {

  val get: ChangeSet = ChangeSet.create("component type enum")(
    sql"""
       create table "component_type"(
         "id" serial not null primary key,
         "name" varchar not null,
         constraint "component_type_name_uniq" unique("name")
       )""",
    sql"""
       INSERT INTO "component_type" ("name")
       VALUES
         ('handlebar'),
         ('seatpost'),
         ('saddle'),
         ('stem'),
         ('frontwheel'),
         ('rearwheel'),
         ('cassette'),
         ('chain'),
         ('brakedisc'),
         ('tire'),
         ('brakepad'),
         ('frontbrake'),
         ('rearbrake'),
         ('fork'),
         ('frontderailleur'),
         ('rearderailleur'),
         ('frontmudguard'),
         ('rearmudguard'),
         ('innertube')
       """
  )
}
