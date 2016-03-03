package com.leagueprojecto.api.services.riot

import akka.actor.Status.Failure
import akka.actor.{ActorRef, ActorLogging, Actor}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
import akka.stream.{Materializer, ActorMaterializer}
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.leagueprojecto.api.services.riot.RiotService.{ServiceNotAvailable, TooManyRequests}

import scala.concurrent.{ExecutionContextExecutor, Future}

object RiotService {
  class ServiceNotAvailable(message: String) extends Exception
  class TooManyRequests(message: String) extends Exception
}

trait RiotService {
  this: Actor with ActorLogging =>

  private val config = context.system.settings.config

  implicit def executor: ExecutionContextExecutor = context.system.dispatcher
  implicit val materializer: Materializer = ActorMaterializer()

  private val hostname: String = config.getString("riot.api.hostname")
  private val port: Int = config.getInt("riot.api.port")
  private val api_key: String = config.getString("riot.api.key")

  val region: String
  val service: String

  lazy val riotConnectionFlow: Flow[HttpRequest, HttpResponse, Any] = {
    val host = hostname.replace(":region", region)

    if (config.getBoolean("riot.api.tls")) {
      Http(context.system).outgoingConnectionTls(host, port)
    } else {
      Http(context.system).outgoingConnection(host, port)
    }
  }

  def endpoint(queryParams: Map[String, String] = Map.empty): Uri = {
    val queryString = (queryParams + ("api_key" -> api_key)).collect { case x => x._1 + "=" + x._2 }.mkString("&")
    val URL = s"/api/lol/$region/$service?$queryString"
    log.debug(s"endpoint: $URL")
    URL
  }

  def riotRequest(httpRequest: HttpRequest): Future[HttpResponse] =
    Source.single(httpRequest).via(riotConnectionFlow).runWith(Sink.head)

  def defaultSuccessHandler(origSender: ActorRef): PartialFunction[HttpResponse, Unit] = {
    case HttpResponse(TooManyRequests, _, _, _) =>
      val message = "Too many requests"
      log.warning(message)
      origSender ! Failure(new TooManyRequests(message))

    case HttpResponse(ServiceUnavailable, _, _, _) =>
      val message = s"Service '$service' not available"
      log.warning(message)
      origSender ! Failure(new ServiceNotAvailable(message))

    case HttpResponse(status, _, _, _) =>
      val message = s"Something went wrong. API call error code: ${status.intValue()}"
      log.warning(message)
      origSender ! Failure(new IllegalStateException(message))
  }

  // Services
  val summonerByName = config.getString("riot.services.summonerbyname.endpoint")
  val matchById = config.getString("riot.services.match.endpoint")
  val matchlistBySummonerId = config.getString("riot.services.matchlist.endpoint")
}