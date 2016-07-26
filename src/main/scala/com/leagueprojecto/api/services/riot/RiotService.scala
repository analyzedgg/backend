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

  private val port: Int = config.getInt("riot.api.port")
  private val api_key: String = config.getString("riot.api.key").trim

  protected var region: String = ""
  protected var service: String = ""

  protected def riotConnectionFlow(hostType: String): Flow[HttpRequest, HttpResponse, Any] = {
    val hostname: String = config.getString(s"riot.api.hostname.$hostType")
    val host = hostname.replace(":region", region)

    if (config.getBoolean("riot.api.tls")) {
      Http(context.system).outgoingConnectionTls(host, port)
    } else {
      Http(context.system).outgoingConnection(host, port)
    }
  }

  protected def endpoint(prefix: String, regionParam: String, serviceParam: String, queryParams: Map[String, String] = Map.empty): Uri = {
    region = regionParam
    service = serviceParam

    val queryString = (queryParams + ("api_key" -> api_key)).collect { case x => x._1 + "=" + x._2 }.mkString("&")
    val URL = s"/$prefix/$region/$service?$queryString"
    log.debug(s"endpoint: $URL")
    URL
  }

  protected def riotRequest(httpRequest: HttpRequest, hostType: String = "api"): Future[HttpResponse] =
    Source.single(httpRequest).via(riotConnectionFlow(hostType)).runWith(Sink.head)

  protected def defaultSuccessHandler(origSender: ActorRef): PartialFunction[HttpResponse, Unit] = {
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
  val championByTags = config.getString("riot.services.championByTags.endpoint")
  val summonerByName = config.getString("riot.services.summonerbyname.endpoint")
  val matchById = config.getString("riot.services.match.endpoint")
  val matchListBySummonerId = config.getString("riot.services.matchlist.endpoint")
}
