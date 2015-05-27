package com.leagueprojecto.api

import akka.actor._
import akka.event.Logging
import akka.http.Http
import akka.http.marshallers.sprayjson.SprayJsonSupport._
import akka.http.model.HttpResponse
import akka.http.model.StatusCodes._
import akka.http.model.headers.RawHeader
import akka.http.server.Directives._
import akka.http.server.ExceptionHandler
import akka.stream.ActorFlowMaterializer
import akka.util.Timeout
import com.leagueprojecto.api.domain.Summoner
import com.leagueprojecto.api.services.riot.{RiotService, SummonerService}
import com.leagueprojecto.api.services.riot.SummonerService.GetSummonerByName
import com.typesafe.config.ConfigFactory

import akka.pattern.ask

import scala.concurrent.duration._

object Startup extends App with JsonProtocols {
  implicit val system = ActorSystem("api")
  implicit val timeout: Timeout = 2.seconds

  implicit val executor = system.dispatcher
  implicit val materializer = ActorFlowMaterializer()

  val config = ConfigFactory.load()
  val logger = Logging(system, getClass)

  val regionMatcher = config.getString("riot.regions").r

  val summonerService: ActorRef = system.actorOf(SummonerService.props)

  val optionsSupport = {
    options {
      complete("")
    }
  }

  implicit def myExceptionHandler = ExceptionHandler {
    case e: SummonerService.SummonerNotFound  => complete(HttpResponse(NotFound))
    case e: RiotService.ServiceNotAvailable   => complete(HttpResponse(ServiceUnavailable))
    case _                                    => complete(HttpResponse(InternalServerError))
  }

  val corsHeaders = List(RawHeader("Access-Control-Allow-Origin", "*"),
    RawHeader("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS, DELETE"),
    RawHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Authorization"))

  def summonerRoute(region: String) = {
    pathPrefix("summoner" / Segment) { name =>
      pathEndOrSingleSlash {
        get {
          complete {
            (summonerService ? GetSummonerByName(region, name)).mapTo[Summoner]
          }
        } ~ optionsSupport
      }
    }
  }

  val routes = {
    logRequestResult("API-service") {
      respondWithHeaders(corsHeaders) {
        pathPrefix("api" / regionMatcher) { region =>
          summonerRoute(region)
        }
      }
    }
  }

  // Bind the HTTP endpoint. Specify http.interface and http.port in the configuration
  // to change the address and port to bind to.
  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
}
