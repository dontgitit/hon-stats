package models

import play.api.libs.json.{JsObject, JsArray, JsValue, Json}

case class MatchStats(
  matchSettings: MatchSettings,
  playerInventories: Seq[PlayerInventory],
  playerMatchStatisticsStrings: Seq[JsObject],
  matchSummary: MatchSummary
)

object MatchStats {
  implicit val msFormat = Json.format[MatchStats]

  def fromSingleMatchStats(jsValue: JsValue): MatchStats = {
    val matchStatsArray = jsValue.as[JsArray]
    val settings = matchStatsArray(0).as[Seq[MatchSettings]]
    val inventories = matchStatsArray(1).as[Seq[PlayerInventory]]
    val stats = matchStatsArray(2).as[Seq[JsObject]]
    val summary = matchStatsArray(3).as[Seq[MatchSummary]]
    MatchStats(settings.head, inventories, stats, summary.head)
  }
}