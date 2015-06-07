package com.leagueprojecto.api

import akka.actor.{ActorSystem, ActorRef}
import akka.event.LoggingAdapter
import akka.http.model.HttpResponse
import akka.http.model.StatusCodes._
import akka.http.model.headers.RawHeader
import akka.http.server.Directives._
import akka.http.server.ExceptionHandler
import akka.pattern.ask
import akka.util.Timeout
import com.leagueprojecto.api.domain.{MatchHistory, Summoner}
import com.leagueprojecto.api.services.CacheService.CachedResponse
import com.leagueprojecto.api.services.riot.MatchHistoryService.GetMatchHistory
import com.leagueprojecto.api.services.riot.SummonerService.GetSummonerByName
import com.leagueprojecto.api.services.riot.{RiotService, SummonerService}
import com.typesafe.config.Config
import akka.http.marshallers.sprayjson.SprayJsonSupport._
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

trait Routes extends JsonProtocols {
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val timeout: Timeout = Timeout(5.seconds)

  def config: Config
  val logger: LoggingAdapter

  val cachedSummonerService: ActorRef
  val cachedMatchHistoryService: ActorRef

  def regionMatcher = config.getString("riot.regions").r

  val optionsSupport = {
    options {
      complete("")
    }
  }

  implicit def myExceptionHandler = ExceptionHandler {
    case e: SummonerService.SummonerNotFound  => complete(HttpResponse(NotFound))
    case e: RiotService.ServiceNotAvailable   => complete(HttpResponse(ServiceUnavailable))
    case e: RiotService.TooManyRequests       => complete(HttpResponse(TooManyRequests))
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
            (cachedSummonerService ? GetSummonerByName(region, name)).mapTo[CachedResponse[Summoner]]
          }
        } ~ optionsSupport
      }
    }
  }

  def matchhistoryRoute(implicit region: String) = {
    pathPrefix("matchhistory" / LongNumber) { summonerId =>
      pathEndOrSingleSlash {
        get {
          complete {
            (cachedMatchHistoryService ? GetMatchHistory(region, summonerId)).mapTo[CachedResponse[List[MatchHistory]]]
          }
        } ~ optionsSupport
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
