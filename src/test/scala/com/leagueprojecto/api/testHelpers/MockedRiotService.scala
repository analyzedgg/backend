package com.leagueprojecto.api.testHelpers

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, Materializer}
import com.leagueprojecto.api.services.riot.RiotService

import scala.concurrent.{ExecutionContextExecutor, Future}

trait MockedRiotService extends RiotService {
  this: Actor with ActorLogging =>

  implicit override def executor: ExecutionContextExecutor = context.system.dispatcher
  implicit override val materializer: Materializer = ActorMaterializer()

  override protected def endpoint(prefix: String, regionParam: String, serviceParam: String, queryParams: Map[String, String] = Map.empty): Uri = {
    val queryString = queryParams.collect { case x => x._1 + "=" + x._2 }.mkString("&")
    s"/$prefix/$region/$service?$queryString"
  }

  override protected def riotRequest(httpRequest: HttpRequest, hostType: String = "api"): Future[HttpResponse] =
    Source.single(httpRequest).via(riotConnectionFlow(hostType)).runWith(Sink.head)

}
