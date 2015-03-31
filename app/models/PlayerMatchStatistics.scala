package models

import play.api.libs.json.{JsObject, JsValue, Json}

case class PlayerMatchStatistics(
  herodmg: Long,
  bdmg: Long,
  team: Team,
  nickname: String,
  hero: Hero
)

object PlayerMatchStatistics {
  implicit val playerMatchStatisticsFormat = Json.format[PlayerMatchStatistics]

  protected def f(js: JsValue, field: String) = (js \ field).as[String]

  def fromPlayerMatchStatisticsJs(matchStats: JsObject, heroes: Map[String, Hero]) = {
    val team = (matchStats \ "team").as[String]
    val teamName = team match {
      case "1" => Team.Legion
      case "2" => Team.Hellbourne
    }
    PlayerMatchStatistics(f(matchStats, "herodmg").toLong, f(matchStats, "bdmg").toLong, teamName, f(matchStats, "nickname"), heroes(f(matchStats, "hero_id")))
  }
}

//case class PlayerMatchStatisticsStrings(
//  match_id: String,
//  account_id: String,
//  hero_id: String,
//  position: String,
//  team: String,
//  level: String,
//  wins: String,
//  losses: String,
//  concedes: String,
//  concedevotes: String,
//  buybacks: String,
//  herokills: String,
//  herodmg: String,
//  heroassists: String,
//  deaths: String,
//  teamcreepkills: String,
//  neutralcreepkills: String,
//  bdmg: String,
//  bgold: String,
//  denies: String,
//  gold: String,
//  exp: String,
//  wards: String,
//  bloodlust: String,
//  doublekill: String,
//  triplekill: String,
//  quadkill: String,
//  annihilation: String,
//  ks3: String,
//  ks4: String,
//  ks5: String,
//  ks6: String,
//  ks7: String,
//  ks8: String,
//  ks9: String,
//  ks10: String,
//  ks15: String,
//  used_token: String,
//  nickname: String
//) {
//
//}
//
//object PlayerMatchStatisticsStrings {
//  implicit val playerMatchStatisticsStringsFormat = Json.format[PlayerMatchStatisticsStrings]
//}