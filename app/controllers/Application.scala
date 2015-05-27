package controllers

import java.util.concurrent.{SynchronousQueue, TimeUnit, ThreadPoolExecutor}

import models._
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import play.api.libs.ws._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

import play.api.libs.json._
import play.api.Play
//import scala.concurrent.ExecutionContext.Implicits.global


object Application extends Controller {
  lazy val executor =
    new ThreadPoolExecutor(
      0,
      5000, // 5000 threads, maxes out at ~10K queues per second, up to 500K queues at a time - dies after for some reason
      60L,
      TimeUnit.SECONDS,
      new SynchronousQueue[Runnable]()
    )

  implicit val myExecutionContext: ExecutionContext =
    ExecutionContext.fromExecutorService(executor)

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
      try {
        MatchStats.fromSingleMatchStats(response.json)
      } catch {
        case t: Throwable =>
          Console.println(s"Unable to parse match stats; got body ${response.body}")
          throw t
      }
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

  def getItemIdNames = {
    Console.println(s"getting item id names...")
    val holder = WS.url(s"https://api.heroesofnewerth.com/items/all/?token=$honApiToken")
    val response = holder.get
    response.map { r =>
      r.json.as[JsObject].keys
    }
  }
  lazy val itemIdNames = Await.result(getItemIdNames, Duration.Inf)

  def getItem(name: String): Future[Option[HoNItem]] = {
    val url = s"https://api.heroesofnewerth.com/items/name/$name/?token=$honApiToken"
    val holder = WS.url(url)
    val ft = holder.get().flatMap { response =>
      response.status match {
        case http.Status.NOT_FOUND =>
          Console.println(s"$name not found; must be old item")
          Future.successful(None)

        case http.Status.TOO_MANY_REQUEST =>
          val sleepSecs = 15
          Console.println(s"getting throttled in $name; sleeping for $sleepSecs seconds")
          Thread.sleep(sleepSecs * 1000)
          val ft = getItem(name)
          ft.onFailure { case t: Throwable =>
            Console.println(s"Inner ft failed for $name; $t")
          }
          ft

        case _ =>
          val ftthing = Future {
            Some(response.json.as[HoNItem])
          }
          ftthing.onFailure { case t: Throwable =>
            Console.println(s"ft failed: $t")
          }
          ftthing
      }
    }
    ft.onFailure { case t: Throwable =>
      Console.println(s"Failed to get item $name; $t")
    }
    ft
  }

  lazy val items = {
    val fts = itemIdNames.map { itemIdName => getItem(itemIdName) }
    val ft = Future.sequence(fts)
    val mostItems = Await.result(ft, Duration.Inf).filter(_.nonEmpty).map { itemOpt =>
      itemOpt.get.item_id -> itemOpt.get
    }.toMap
    Map(
      "150" -> HoNItem(item_id = "150", attributes = HoNItemAttributes(icon = "", name = "Ioyn Stone")),
      "151" -> HoNItem(item_id = "151", attributes = HoNItemAttributes(icon = "", name = "Spell Sunder")),
      "152" -> HoNItem(item_id = "152", attributes = HoNItemAttributes(icon = "", name = "Veiled Rot")),
      "168" -> HoNItem(item_id = "168", attributes = HoNItemAttributes(icon = "", name = "Merrick's Bounty"))
    ) ++ mostItems
  }

  protected def augmentStats(stats: JsObject) = {
    val heroId = (stats \ ApiFields.hero_id.toString).as[String]

    val itemSlotNames = 1.to(6).map { slotNumber =>
      val slotValue = stats \ s"slot_$slotNumber"
      slotNumber -> slotValue.asOpt[String]
    }.filter(_._2.nonEmpty).map { case (slotNumber, slotOpt) =>
      s"slot_${slotNumber}_name" -> items(slotOpt.get).attributes.name
    }.toMap

    stats.+("hero_name", Json.toJson(heroes(heroId).disp_name))
         .++(Json.toJson(itemSlotNames).as[JsObject])
  }

  def matchStatsToAugmentedPlayerData(matchStats: MatchStats): Seq[JsObject] = {
    matchStats.playerInventories.map { player =>
      val stats = matchStats.playerMatchStatisticsStrings.find { playerStatsData =>
        (playerStatsData \ ApiFields.account_id.toString).as[String] == player.account_id
      }.head
      val mixedData = Json.toJson(player).as[JsObject] ++ stats
      augmentStats(mixedData)
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
