package com.leagueprojecto.api

import akka.actor._
import akka.event.Logging
import akka.http.Http
import akka.http.marshallers.sprayjson.SprayJsonSupport._
import akka.http.model.headers.RawHeader
import akka.http.server.Directives._
import akka.stream.ActorFlowMaterializer
import akka.util.Timeout
import com.leagueprojecto.api.services.MatchesService
import com.typesafe.config.ConfigFactory

import akka.pattern.ask

import scala.concurrent.duration._

object Startup extends App with JsonProtocols {
  implicit val system = ActorSystem("api")
  implicit val timeout: Timeout = 2.seconds

  implicit val executor = system.dispatcher
  implicit val materializer = ActorFlowMaterializer()

  val matchesService: ActorRef = system.actorOf(MatchesService.props)

  val optionsSupport = {
    options {complete("")}
  }

  val corsHeaders = List(RawHeader("Access-Control-Allow-Origin", "*"),
    RawHeader("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS, DELETE"),
    RawHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Authorization"))

  val matchesRoute = {
    respondWithHeaders(corsHeaders) {
      pathPrefix("api" / "matches") {
        pathEndOrSingleSlash {
          get {
            complete {
              (matchesService ? MatchesService.GetMatches).mapTo[MatchesService.Matches]
            }
          } ~ optionsSupport
        } ~ path(Segment / Segment) { (region, summonerName) =>
          get {
            complete {
              val message = MatchesService.GetMatches(region, summonerName)
              (matchesService ? message).mapTo[MatchesService.Matches]
            }
          } ~ optionsSupport
        }
      }
    }
  }

  val routes = {
    logRequestResult("API-service") {
      matchesRoute
    }
  }

  val config = ConfigFactory.load()
  val logger = Logging(system, getClass)

  // Bind the HTTP endpoint. Specify http.interface and http.port in the configuration
  // to change the address and port to bind to.
  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
}
