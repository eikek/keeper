package keeper.bikes.db.migration

import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

final case class DbSchemaHistory(
    id: Int,
    name: String
)

object DbSchemaHistory:
  val createTable: Command[Void] =
    sql"""create table if not exists "db_schema_history"(
            "id" int4 not null primary key,
            "name" varchar not null,
            "created_at" timestamptz not null default now()
       )""".command

  val exists: Query[DbSchemaHistory, Boolean] =
    sql"SELECT count(id) FROM db_schema_history WHERE id = $int4"
      .query(int8)
      .contramap[DbSchemaHistory](_.id)
      .map(_ > 0)

  val insert: Command[DbSchemaHistory] =
    sql"INSERT INTO db_schema_history (id, name) VALUES ($int4, $varchar)".command
      .contramap(e => e.id *: e.name *: EmptyTuple)
