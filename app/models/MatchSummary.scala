package models

import play.api.libs.json.Json

case class MatchSummary(
  match_id: String,
  server_id: String,
  map: HoNMap,
  time_played: String,
  c_state: String,
  version: String,
  mdt: String,
  replay_url: String
)

object MatchSummary {
  implicit val msFormat = Json.format[MatchSummary]
}