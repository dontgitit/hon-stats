package models

import play.api.libs.json.Json

case class HoNItemAttributes(
  icon: String,
  name: String
)
object HoNItemAttributes {
  implicit val hiaFormat = Json.format[HoNItemAttributes]
}

case class HoNItem(
  item_id: String,
  attributes: HoNItemAttributes
)

object HoNItem {
  implicit val itemFormat = Json.format[HoNItem]
}