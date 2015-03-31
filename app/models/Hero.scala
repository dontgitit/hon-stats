package models

import play.api.libs.json.Json

case class Hero(
  hero_id: String,
  disp_name: String
)

object Hero {
  implicit val heroFormat = Json.format[Hero]
}