package controllers

import models._
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import play.api.libs.ws._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

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
    Console.println(s"getting heroes...")
    val holder = WS.url(s"https://api.heroesofnewerth.com/heroes/all?token=$honApiToken")
    val response = holder.get
    response.map { r =>
      Json.fromJson[Map[String, Map[String, Hero]]](r.json).get.map { case (heroName, heroIdToHero) =>
        heroIdToHero.head
      }
    }
  }
  lazy val heroes = Await.result(getHeroes, Duration.Inf)

  protected def augmentStats(stats: JsObject) = {
    val heroId = (stats \ ApiFields.hero_id.toString).as[String]
    stats.+("hero_name", Json.toJson(heroes(heroId).disp_name))
  }

  def matchStatsToAugmentedPlayerData(matchStats: MatchStats): Seq[JsObject] = {
    val playerData = matchStats.playerInventories.map { player =>
      val stats = matchStats.playerMatchStatisticsStrings.find { playerStatsData =>
        (playerStatsData \ ApiFields.account_id.toString).as[String] == player.account_id
      }.head
      val augmentedStats = augmentStats(stats)
      player -> augmentedStats
    }
    playerData.map { case (inventory, other) =>
      Json.toJson(inventory).as[JsObject] ++ other
    }
  }

  def matchStats = Action.async { implicit request =>
    matchForm.bindFromRequest.fold(
      errors => Future.successful(BadRequest(views.html.index(errors))),
      id => {
        getMatchStats(id).map { matchStats =>
          val playerStatsStrings = matchStats.playerMatchStatisticsStrings
          val playerStats = playerStatsStrings.map { pss =>
            PlayerMatchStatistics.fromPlayerMatchStatisticsJs(pss, heroes)
          }
          val (legionStats, hellbourneStats) = playerStats.partition(_.team == Team.Legion)

          Ok(views.html.stats(legionStats, hellbourneStats))
        }
      }
    )
  }

  def playerDataToCsv(playerData: Seq[JsObject]) = {
    val headers = playerData.map(_.keys).flatten.toSet.toList.sorted
    val headerRow = headers.mkString(",")
    val playerRows = playerData.map { pd =>
      val rowVals = headers.map { key =>
        (pd \ key).asOpt[String].getOrElse("")
      }
      rowVals.mkString(",")
    }
    val rows = Seq(headerRow) ++ playerRows
    rows.mkString("\n") + "\n"
  }

  def matchCsv(id: String) = Action.async { implicit request =>
    getMatchStats(id).map { matchStats =>
      val playerData = matchStatsToAugmentedPlayerData(matchStats)
      val csvData = playerDataToCsv(playerData)
      val fileName = s"hon_$id.csv"
      Ok(csvData).withHeaders(
        CONTENT_TYPE -> "text/csv",
        CONTENT_DISPOSITION -> ("attachment; filename=\"" + fileName + "\"")
      )
    }
  }

}