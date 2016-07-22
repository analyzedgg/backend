package com.leagueprojecto.api.services.riot

import akka.actor.{ActorLogging, ActorRef, Props, Actor}
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.leagueprojecto.api.JsonProtocols
import com.leagueprojecto.api.domain.Summoner
import spray.json._

object SummonerService {
  case class GetSummonerByName(region: String, name: String)
  case class SummonerNotFound(message: String) extends Exception
  case class Result(summoner: Summoner)

  def props = Props(new SummonerService)
}

class SummonerService extends Actor with ActorLogging with RiotService with JsonProtocols {
  import SummonerService._

  override def receive: Receive = {
    case GetSummonerByName(regionParam: String, name: String) =>
      val origSender: ActorRef = sender()

      val summonerEndpoint: Uri = endpoint(regionParam, summonerByName + name)

      val future = riotRequest(RequestBuilding.Get(summonerEndpoint))
      future onSuccess successHandler(origSender).orElse(defaultSuccessHandler(origSender))
      future onFailure failureHandler(origSender)
  }

  def successHandler(origSender: ActorRef): PartialFunction[HttpResponse, Unit] = {
    case HttpResponse(OK, _, entity, _) =>
      Unmarshal(entity).to[String].onSuccess {
        case result: String =>
          val summoner = transform(result.parseJson.asJsObject)
          origSender ! Result(summoner)
      }

    case HttpResponse(NotFound, _, _, _) =>
      val message = s"No summoner found by service '$service' for region '$region'"
      log.warning(message)
      origSender ! SummonerNotFound(message)
  }

  def failureHandler(origSender: ActorRef): PartialFunction[Throwable, Unit] = {
    case e: Exception =>
      log.error(s"GetSummonerByName request failed for reason: ${e.getMessage}")
  }

  private def transform(riotResult: JsObject): Summoner = {
    val firstKey = riotResult.fields.keys.head
    riotResult.fields(firstKey).convertTo[Summoner]
  }
}
