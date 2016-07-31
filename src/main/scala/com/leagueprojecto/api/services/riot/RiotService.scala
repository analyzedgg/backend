package com.leagueprojecto.api.services.riot

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.leagueprojecto.api.services.riot.RiotService.{ServiceNotAvailable, TooManyRequests}

import scala.concurrent.{ExecutionContextExecutor, Future}

object RiotService {

  class ServiceNotAvailable(message: String) extends Exception

  class TooManyRequests(message: String) extends Exception

}

trait RiotService {
  this: Actor with ActorLogging =>

  val objectMapper = new ObjectMapper with ScalaObjectMapper
  objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  objectMapper.registerModule(DefaultScalaModule)

  private val config = context.system.settings.config

  implicit def executor: ExecutionContextExecutor = context.system.dispatcher

  implicit val materializer: Materializer = ActorMaterializer()

  private val hostname: String = config.getString("riot.api.hostname")
  private val port: Int = config.getInt("riot.api.port")
  private val api_key: String = config.getString("riot.api.key").trim

  private[this] def riotConnectionFlow(region: String, service: String): Flow[HttpRequest, HttpResponse, Any] = {
    val host = hostname.replace(":region", region)

    if (config.getBoolean("riot.api.tls")) {
      Http(context.system).outgoingConnectionTls(host, port)
    } else {
      Http(context.system).outgoingConnection(host, port)
    }
  }

  protected def riotGetRequest(prefix: String, regionParam: String, serviceParam: String, queryParams: Map[String, String] = Map.empty): Future[HttpResponse] = {
    val queryString = (queryParams + ("api_key" -> api_key)).collect { case x => x._1 + "=" + x._2 }.mkString("&")
    val URL = s"/$prefix/$region/$service?$queryString"
    log.debug(s"endpoint: $URL")
    Source.single(RequestBuilding.Get(URL)).via(riotConnectionFlow(regionParam, serviceParam)).runWith(Sink.head)
  }

  protected def mapRiotTo[R](response: ResponseEntity, responseClass: Class[R]): Future[R] = {
    Unmarshal(response).to[String].map { mappedResult =>
      log.debug(s"Got json string $mappedResult")
      objectMapper.readValue(mappedResult, responseClass)
    }
  }

  // Services
  val championByTags = config.getString("riot.services.championByTags.endpoint")
  val summonerByName = config.getString("riot.services.summonerbyname.endpoint")
  val matchById = config.getString("riot.services.match.endpoint")
  val matchlistBySummonerId = config.getString("riot.services.matchlist.endpoint")
  val matchListBySummonerId = config.getString("riot.services.matchlist.endpoint")
}