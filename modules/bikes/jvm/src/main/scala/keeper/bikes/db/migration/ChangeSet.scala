package keeper.bikes.db.migration

import skunk.*

final case class ChangeSet(
    name: String,
    statements: Seq[Command[Void]]
)

object ChangeSet:
  def cmd(name: String)(statements: Command[Void]*): ChangeSet =
    ChangeSet(name, statements)

  def create(name: String)(statements: Fragment[Void]*): ChangeSet =
    ChangeSet(name, statements.map(_.command))
