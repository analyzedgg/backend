package com.leagueprojecto.api.services.riot

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.{HttpResponse, Uri}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.leagueprojecto.api.JsonProtocols
import com.leagueprojecto.api.domain.Champion
import com.leagueprojecto.api.services.riot.ChampionService.{GetChampions, ChampionsResponse}
import spray.json._

object ChampionService {

  case class GetChampions(region: String)
  case class ChampionsResponse(data: Map[String, Champion])

  def props = Props[ChampionService]
}

class ChampionService extends Actor with ActorLogging with RiotService with JsonProtocols {
  override def receive: Receive = {
    case GetChampions(regionParam: String) =>
      val origSender: ActorRef = sender()
      val championEndpoint: Uri = endpoint("api/lol/static-data", regionParam, championByTags, Map(("champData", "tags")))

      val future = riotRequest(RequestBuilding.Get(championEndpoint), "global")
      future onSuccess successHandler(origSender).orElse(defaultSuccessHandler(origSender))
      future onFailure failureHandler(origSender)
  }

  def successHandler(origSender: ActorRef): PartialFunction[HttpResponse, Unit] = {
    case HttpResponse(OK, _, entity, _) =>
      Unmarshal(entity).to[String].onSuccess {
        case result: String =>
          val championList = transform(result.parseJson.asJsObject)
          origSender ! championList
      }
  }

  def failureHandler(origSender: ActorRef): PartialFunction[Throwable, Unit] = {
    case e: Exception =>
      log.error(s"GetSummonerByName request failed for reason: $e")
  }

  private def transform(riotResult: JsObject): ChampionsResponse = {
    riotResult.convertTo[ChampionsResponse]
  }
}