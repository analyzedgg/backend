package com.leagueprojecto.api

import akka.actor.{ActorRef, ActorSystem}
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.pattern.{CircuitBreaker, ask}
import akka.util.Timeout
import com.leagueprojecto.api.services.{MatchHistoryManager, SummonerManager}
import com.leagueprojecto.api.services.SummonerManager.GetSummoner
import com.leagueprojecto.api.services.riot.{ChampionService, RiotService, SummonerService}
import com.typesafe.config.Config
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import com.leagueprojecto.api.services.riot.ChampionService.GetChampions

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

trait Routes extends JsonProtocols {
  implicit val system: ActorSystem

  implicit def executor: ExecutionContextExecutor

  implicit val timeout: Timeout = Timeout(1.minute)

  lazy val couchDbCircuitBreaker =
    new CircuitBreaker(system.scheduler, maxFailures = 5, callTimeout = 5.seconds, resetTimeout = 1.minute)(executor)

  def config: Config

  val logger: LoggingAdapter

  def regionMatcher = config.getString("riot.regions").r

  def queueMatcher = config.getString("riot.queueTypes")

  val optionsSupport = {
    options {
      complete("")
    }
  }

  implicit def myExceptionHandler = ExceptionHandler {
    case e: RiotService.ServiceNotAvailable => complete(HttpResponse(ServiceUnavailable))
    case e: RiotService.TooManyRequests => complete(HttpResponse(TooManyRequests))
    case e: SummonerService.SummonerNotFound => complete(HttpResponse(NotFound))
    case _ => complete(HttpResponse(InternalServerError))
  }

  val corsHeaders = List(RawHeader("Access-Control-Allow-Origin", "*"),
    RawHeader("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS, DELETE"),
    RawHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Authorization"))

  def championsRoute(implicit region: String) = {
    pathPrefix("champions") {
      pathEndOrSingleSlash {
        get {
          complete {
            val championManager = createChampionActor
            val future = championManager ? GetChampions(region)
            future.mapTo[ChampionService.ChampionsResponse].map(_.data)
          }
        } ~ optionsSupport
      }
    }
  }

  def summonerRoute(implicit region: String) = {
    pathPrefix("summoner" / Segment) { name =>
      pathEndOrSingleSlash {
        get {
          complete {
            val summonerManager = createSummonerActor
            val future = summonerManager ? GetSummoner(region, name)
            future.mapTo[SummonerManager.Result].map(_.summoner)
          }
        } ~ optionsSupport
      }
    }
  }

  def matchHistoryRoute(implicit region: String) = {
    pathPrefix("matchhistory" / LongNumber) { summonerId =>
      parameters("queue" ? "", "champions" ? "") { (queueParam: String, championParam: String) =>
        var queueType = queueParam
        if (!queueParam.matches(queueMatcher)) queueType = ""

        var championList = championParam
        if (!championParam.matches("^(\\d{1,3}|,*)*$")) championList = ""

        pathEndOrSingleSlash {
          get {
            complete {
              val matchHistoryManager = createMatchHistoryActor
              val future = matchHistoryManager ? MatchHistoryManager.GetMatches(region, summonerId, queueParam, championParam)
              future.mapTo[MatchHistoryManager.Result].map(_.data)
            }
          } ~ optionsSupport
        }
      }
    }
  }


  val routes = {
    //  logRequestResult("API-service") {
    respondWithHeaders(corsHeaders) {
      pathPrefix("api" / regionMatcher) { regionSegment =>
        implicit val region = regionSegment.toLowerCase

        championsRoute ~ summonerRoute ~ matchHistoryRoute
      }
    }
  }

  private[this] def createChampionActor: ActorRef = system.actorOf(ChampionService.props)
  private[this] def createSummonerActor: ActorRef = system.actorOf(SummonerManager.props(couchDbCircuitBreaker))
  private[this] def createMatchHistoryActor: ActorRef = system.actorOf(MatchHistoryManager.props(couchDbCircuitBreaker))

}
