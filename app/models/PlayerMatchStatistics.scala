package models

import play.api.libs.json.Json

case class PlayerMatchStatistics(
  herodmg: Long,
  bdmg: Long,
  team: Team,
  nickname: String,
  hero: Hero
)

object PlayerMatchStatistics {
  implicit val playerMatchStatisticsFormat = Json.format[PlayerMatchStatistics]
}

case class PlayerMatchStatisticsStrings(
  herodmg: String,
  bdmg: String,
  team: String,
  nickname: String,
  hero_id: String
) {
  def toPlayerMatchStatistics(heroes: Map[String, Hero]) = {
    val teamName = team match {
      case "1" => Team.Legion
      case "2" => Team.Hellbourne
    }
    PlayerMatchStatistics(herodmg.toLong, bdmg.toLong, teamName, nickname, heroes(hero_id))
  }
}

object PlayerMatchStatisticsStrings {
  implicit val playerMatchStatisticsStringsFormat = Json.format[PlayerMatchStatisticsStrings]
}