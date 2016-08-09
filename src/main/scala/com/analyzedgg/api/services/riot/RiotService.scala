package com.analyzedgg.api.services.riot

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
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.analyzedgg.api.domain.riot.{RiotSummoner, RiotSummonerDeserializer}

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

  val riotChampionModule: SimpleModule = new SimpleModule()
  riotChampionModule.addDeserializer(classOf[RiotSummoner], new RiotSummonerDeserializer())
  objectMapper.registerModule(riotChampionModule)

  private val config = context.system.settings.config

  implicit def executor: ExecutionContextExecutor = context.system.dispatcher

  implicit val materializer: Materializer = ActorMaterializer()

  private val port: Int = config.getInt("riot.api.port")
  private val api_key: String = config.getString("riot.api.key").trim

  private[this] def riotConnectionFlow(region: String, service: String, hostType: String): Flow[HttpRequest, HttpResponse, Any] = {
    val hostname: String = config.getString(s"riot.api.hostname.$hostType")
    val host = hostname.replace(":region", region)

    log.debug(s"endpoint host: $host")

    if (config.getBoolean("riot.api.tls")) {
      Http(context.system).outgoingConnectionTls(host, port)
    } else {
      Http(context.system).outgoingConnection(host, port)
    }
  }

  protected def riotGetRequest(regionParam: String, serviceParam: String, queryParams: Map[String, String] = Map.empty,
                               prefix: String = "api/lol", hostType: String = "api"): Future[HttpResponse] = {
    val queryString = (queryParams + ("api_key" -> api_key)).collect { case x => x._1 + "=" + x._2 }.mkString("&")
    val URL = s"/$prefix/$regionParam/$serviceParam?$queryString"
    log.debug(s"endpoint: $URL")
    Source.single(RequestBuilding.Get(URL)).via(riotConnectionFlow(regionParam, serviceParam, hostType)).runWith(Sink.head)
  }

  protected def mapRiotTo[R](response: ResponseEntity, responseClass: Class[R]): Future[R] = {
    Unmarshal(response).to[String].map { mappedResult =>
      log.debug(s"Got json string $mappedResult")
      objectMapper.readValue[R](mappedResult, responseClass)
    }
  }

  // Services
  val championByTags = config.getString("riot.services.championByTags.endpoint")
  val summonerByName = config.getString("riot.services.summonerbyname.endpoint")
  val matchById = config.getString("riot.services.match.endpoint")
  val matchListBySummonerId = config.getString("riot.services.matchlist.endpoint")
}