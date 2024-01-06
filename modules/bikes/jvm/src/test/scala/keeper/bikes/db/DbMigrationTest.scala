package keeper.bikes.db

import munit.CatsEffectSuite
import skunk.codec.all.int8
import skunk.implicits.sql

class DbMigrationTest extends CatsEffectSuite with PostgresTest {

  test("migration") {
    session.use { s =>
      assertIO(
        s.unique(sql"SELECT count(*) FROM brand".query(int8)),
        58L
      )
    }
  }

}
