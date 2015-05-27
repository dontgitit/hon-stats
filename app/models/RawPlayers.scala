package models

import scala.slick.driver.PostgresDriver.simple._
import org.joda.time.DateTime
import Database.dynamicSession

case class RawPlayer(
  id: Long,
  data: String,
  updated: DateTime
)

class RawPlayers(tag: Tag) extends Table[RawPlayer](tag, "raw_players") with TableT {
  def id = column[Long]("id", O.PrimaryKey)
  def data = column[String]("data")
  def updated = column[DateTime]("updated")
  def * = (id, data, updated) <> (RawPlayer.tupled, RawPlayer.unapply)
}
