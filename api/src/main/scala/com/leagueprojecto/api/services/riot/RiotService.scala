package com.leagueprojecto.api.services.riot

import akka.actor.Actor
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
import akka.stream.{ActorFlowMaterializer, FlowMaterializer}
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.{ExecutionContextExecutor, Future}

object RiotService {
  class ServiceNotAvailable(message: String) extends Exception
  class TooManyRequests(message: String) extends Exception
}

trait RiotService {
  this: Actor =>

  private val config = context.system.settings.config

  implicit def executor: ExecutionContextExecutor = context.system.dispatcher
  implicit val materializer: FlowMaterializer = ActorFlowMaterializer()

  private val hostname: String = config.getString("riot.api-hostname")
  private val port: Int = config.getInt("riot.api-port")
  private val api_key: String = config.getString("riot.api-key")

  val region: String
  val service: String

  lazy val riotConnectionFlow: Flow[HttpRequest, HttpResponse, Any] =
    Http(context.system).outgoingConnectionTls(region + hostname, port)

  def endpoint(queryParams: Map[String, String] = Map.empty): Uri = {
    val queryString = (queryParams + ("api_key" -> api_key)).collect { case x => x._1 + "=" + x._2 }.mkString("&")
    val URL = s"/api/lol/$region/$service?$queryString"
    println(URL)
    URL
  }

  def riotRequest(httpRequest: HttpRequest): Future[HttpResponse] =
    Source.single(httpRequest).via(riotConnectionFlow).runWith(Sink.head)

  // Services
  val summonerByName = config.getString("riot.services.summonerbyname.endpoint")
  val matchHistoryBySummonerId = config.getString("riot.services.matchhistory.endpoint")
}