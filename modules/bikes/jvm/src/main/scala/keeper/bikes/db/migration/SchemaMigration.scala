package keeper.bikes.db.migration

import cats.effect.Sync
import cats.syntax.all.*
import fs2.Stream

import skunk.Session

final class SchemaMigration[F[_]: Sync](session: Session[F], changeSets: Seq[ChangeSet]) {
  private[this] val logger = scribe.cats.effect[F]
  private val createHistoryTable =
    session.execute(DbSchemaHistory.createTable)

  def migrate: F[Unit] =
    for {
      _ <- createHistoryTable
      _ <- Stream
        .emits(changeSets.zipWithIndex)
        .evalMap(applyChangeset.tupled)
        .compile
        .drain
    } yield ()

  private def applyChangeset(cs: ChangeSet, id: Int): F[Unit] = {
    val entry = DbSchemaHistory(id, cs.name)
    val runCs = cs.statements.traverse(session.execute)
    val insertEntry = session.execute(DbSchemaHistory.insert)(entry)
    session.transaction.use { _ =>
      for {
        exists <- session.unique(DbSchemaHistory.exists)(entry)
        _ <-
          if (exists)
            logger.info(s"Skip existing changeset $id/${cs.name}").as(Seq.empty)
          else
            logger.info(s"Run schema changeset: $id/${cs.name}") *> insertEntry *> runCs
      } yield ()
    }
  }
}

object SchemaMigration:
  def apply[F[_]: Sync](session: Session[F]): SchemaMigration[F] =
    new SchemaMigration[F](session, changesets.all)
