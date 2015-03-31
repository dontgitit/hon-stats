package controllers

import models._
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import play.api.libs.ws._
import scala.concurrent.Future

import play.api.libs.json._
import play.api.Play
import scala.concurrent.ExecutionContext.Implicits.global


object Application extends Controller {
  lazy val honApiToken = Play.current.configuration.getString("hon-api-token").getOrElse(throw new IllegalArgumentException(s"You must provide an api token using 'hon-api-token' in application config!"))

  val matchForm = Form(
    "id" -> nonEmptyText
  )

  def index = Action {
    Ok(views.html.index(matchForm))
  }

  def getMatchStats(matchId: String): Future[MatchStats] = {
    val holder = WS.url(s"https://api.heroesofnewerth.com//match/all/matchid/$matchId/?token=$honApiToken")
    val responseFt = holder.get
    responseFt.map { response =>
      MatchStats.fromSingleMatchStats(response.json)
    }
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
          val playerStatsStrings = matchStats.playerMatchStatisticsStrings
          getHeroes.map { heroes =>
            val playerStats = playerStatsStrings.map { pss =>
              PlayerMatchStatistics.fromPlayerMatchStatisticsJs(pss, heroes)
            }
            val (legionStats, hellbourneStats) = playerStats.partition(_.team == Team.Legion)

            Ok(views.html.stats(legionStats, hellbourneStats))
          }
        }
      }
    )
  }

}