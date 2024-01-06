package keeper.bikes.db.migration.changesets

import keeper.bikes.db.migration.ChangeSet

import skunk.implicits.*

object Strava {

  val auth: ChangeSet = ChangeSet.create("strava auth")(
    sql"""
         create table "strava_token"(
           "id" bigserial not null primary key,
           "client_id" varchar not null,
           "token_type" varchar not null,
           "access_token" text not null,
           "refresh_token" text not null,
           "expires_at" timestamptz not null,
           "expires_in" bigint not null,
           "scope" text not null,
           "created_at" timestamptz not null default now()
         )
       """,
    sql"""
       create index if not exists "strava_token_expires_at_idx" on "strava_token"("expires_at")
       """,
    sql"""
       create index if not exists "strava_token_created_at_idx" on "strava_token"("created_at")
       """
  )
}
