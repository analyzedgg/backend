package com.leagueprojecto.api

import akka.actor.{ActorSystem, ActorRef}
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.pattern.ask
import akka.util.Timeout
import com.leagueprojecto.api.domain.{MatchHistory, Summoner}
import com.leagueprojecto.api.services.CacheService.CachedResponse
import com.leagueprojecto.api.services.MatchHistoryManager.GetMatches
import com.leagueprojecto.api.services.SummonerManager
import com.leagueprojecto.api.services.SummonerManager.GetSummoner
import com.leagueprojecto.api.services.riot.{RiotService, SummonerService}
import com.typesafe.config.Config
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

trait Routes extends JsonProtocols {
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val timeout: Timeout = Timeout(1.minute)

  def config: Config
  val logger: LoggingAdapter

  val cachedMatchHistoryService: ActorRef

  def regionMatcher = config.getString("riot.regions").r
  def queueMatcher = config.getString("riot.queueTypes")

  val optionsSupport = {
    options {
      complete("")
    }
  }

  implicit def myExceptionHandler = ExceptionHandler {
    case e: RiotService.ServiceNotAvailable   => complete(HttpResponse(ServiceUnavailable))
    case e: RiotService.TooManyRequests       => complete(HttpResponse(TooManyRequests))
    case e: SummonerService.SummonerNotFound  => complete(HttpResponse(NotFound))
    case _                                    => complete(HttpResponse(InternalServerError))
  }

  val corsHeaders = List(RawHeader("Access-Control-Allow-Origin", "*"),
    RawHeader("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS, DELETE"),
    RawHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Authorization"))

  def summonerRoute(implicit region: String) = {
    pathPrefix("summoner" / Segment) { name =>
      pathEndOrSingleSlash {
        get {
          complete {
            val summonerManager = system.actorOf(SummonerManager.props)
            (summonerManager ? GetSummoner(region, name)).mapTo[Summoner]
          }
        } ~ optionsSupport
      }
    }
  }

  def matchhistoryRoute(implicit region: String) = {
    pathPrefix("matchhistory" / LongNumber) { summonerId =>
      parameters("queue" ? "", "champions" ? "") { (queueParam, championParam) =>
        var queueType = queueParam
        if (!queueParam.matches(queueMatcher)) queueType = ""

        var championList = championParam
        if (!championParam.matches("^(\\d{1,3}|,*)*$")) championList = ""

        pathEndOrSingleSlash {
          get {
            complete {
              (cachedMatchHistoryService ? GetMatches(region, summonerId, queueType, championList)).mapTo[CachedResponse[List[MatchHistory]]]
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

        summonerRoute ~ matchhistoryRoute
      }
    }
  }
}
