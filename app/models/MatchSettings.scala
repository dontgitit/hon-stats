package models

import play.api.libs.json.Json

/**
[ {
"match_id" : "137507908",
"no_repick" : "0",
"no_agi" : "0",
"drp_itm" : "0",
"no_timer" : "0",
"rev_hs" : "0",
"no_swap" : "0",
"no_int" : "0",
"alt_pick" : "0",
"veto" : "0",
"shuf" : "0",
"no_str" : "0",
"no_pups" : "0",
"dup_h" : "0",
"ap" : "0",
"br" : "0",
"em" : "0",
"cas" : "0",
"rs" : "0",
"nl" : "1",
"officl" : "1",
"no_stats" : "0",
"ab" : "0",
"hardcore" : "0",
"dev_heroes" : "0",
"verified_only" : "0",
"gated" : "0",
"rapidfire" : "0"
} ]
    **/

case class MatchSettings(
  match_id: String,
  officl: String,
  no_stats: String
)

object MatchSettings {
  implicit val msFormat = Json.format[MatchSettings]
}