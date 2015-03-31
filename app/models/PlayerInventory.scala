package models

import play.api.libs.json.Json

case class PlayerInventory(
  account_id: String,
  match_id: String,
  slot_1: Option[String],
  slot_2: Option[String],
  slot_3: Option[String],
  slot_4: Option[String],
  slot_5: Option[String],
  slot_6: Option[String],
  nickname: String
)

object PlayerInventory {
  implicit val piFormat = Json.format[PlayerInventory]
}