package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import play.api.libs.ws._
import scala.concurrent.Future

import play.api.libs.json._
import play.api.Play
import scala.concurrent.ExecutionContext.Implicits.global

case class Hero(
  hero_id: String,
  disp_name: String
)
object Hero {
  implicit val heroFormat = Json.format[Hero]
}

case class PlayerMatchStatistics(
  herodmg: Long,
  bdmg: Long,
  team: String,
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
      case "1" => Application.LEGION
      case "2" => Application.HELLBOURNE
    }
    PlayerMatchStatistics(herodmg.toLong, bdmg.toLong, teamName, nickname, heroes(hero_id))
  }
}
object PlayerMatchStatisticsStrings {
  implicit val playerMatchStatisticsStringsFormat = Json.format[PlayerMatchStatisticsStrings]
}

object Application extends Controller {
  lazy val honApiToken = Play.current.configuration.getString("hon-api-token").getOrElse(throw new IllegalArgumentException(s"You must provide an api token using 'hon-api-token' in application config!"))

  val LEGION = "Legion"
  val HELLBOURNE = "Hellbourne"

  val matchForm = Form(
    "id" -> nonEmptyText
  )

  def index = Action {
    Ok(views.html.index(matchForm))
  }

  def getMatchStats(matchId: String): Future[JsValue] = {
    val holder = WS.url(s"https://api.heroesofnewerth.com//match/all/matchid/$matchId/?token=$honApiToken")
    val response = holder.get
    response.map(_.json)
  }

  def getHeroes: Future[Map[String, Hero]] = {
    val holder = WS.url(s"https://api.heroesofnewerth.com/heroes/all?token=$honApiToken")
    val response = holder.get
    response.map { r =>
      Json.fromJson[Map[String, Map[String, Hero]]](r.json).get.map { case (heroName, heroIdToHero) =>
        heroIdToHero.head
      }
    }
  }

  def matchStats = Action.async { implicit request =>
    matchForm.bindFromRequest.fold(
      errors => Future.successful(BadRequest(views.html.index(errors))),
      id => {
        getMatchStats(id).flatMap { matchStats =>
          val matchStatsAsArray = matchStats.as[JsArray]
          val stats = matchStatsAsArray(2)
          val playerStatsStrings = Json.fromJson[Seq[PlayerMatchStatisticsStrings]](stats).get
          getHeroes.map { heroes =>
            val playerStats = playerStatsStrings.map(_.toPlayerMatchStatistics(heroes))
            val (legionStats, hellbourneStats) = playerStats.partition(_.team == LEGION)

            Ok(views.html.stats(legionStats, hellbourneStats, matchStats))
          }
        }
      }
    )
  }

}