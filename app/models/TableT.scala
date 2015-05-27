package models

import org.joda.time.DateTime
import play.api.Play.current
import play.api.db.DB
import java.sql.Timestamp

import scala.slick.driver.PostgresDriver.simple._

trait TableT {
  implicit def dateTime =
    MappedColumnType.base[DateTime, Timestamp](
      dt => new Timestamp(dt.getMillis),
      ts => new DateTime(ts.getTime)
    )

  def db = Database.forDataSource(DB.getDataSource())
}
