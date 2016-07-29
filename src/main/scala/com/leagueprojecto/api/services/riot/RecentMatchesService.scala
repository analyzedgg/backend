package com.leagueprojecto.api.services.riot

import akka.actor.{ActorRef, Props, Actor, ActorLogging}
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpResponse, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.leagueprojecto.api.JsonProtocols
import com.leagueprojecto.api.domain.Match
import spray.json._

object RecentMatchesService {
  case class GetRecentMatchIds(region: String, summonerId: Long, queueType: String, championList: String, amount: Int)
  case class Result(matchIds: Seq[Long])

  def props = Props(new RecentMatchesService)
}

class RecentMatchesService extends Actor with RiotService with ActorLogging with JsonProtocols {
  import RecentMatchesService._

  override def receive: Receive = {
    case GetRecentMatchIds(regionParam, summonerId, queueType, championList, amount) =>
      implicit val origSender = sender()

      val queryParams: Map[String, String] = Map("beginIndex" -> (0 toString), "endIndex" -> (amount toString))
      val matchListEndpoint: Uri = endpoint("api/lol", regionParam, matchListBySummonerId + summonerId, queryParams)

      val future = riotRequest(RequestBuilding.Get(matchListEndpoint))
      future onSuccess successHandler(origSender).orElse(defaultSuccessHandler(origSender))
      future onFailure failureHandler(origSender)
  }

  def successHandler(origSender: ActorRef): PartialFunction[HttpResponse, Unit] = {
    case HttpResponse(OK, _, entity, _) =>
      Unmarshal(entity).to[String].onSuccess {
        case result: String =>
          val matchList = transform(result.parseJson.asJsObject)
          val matchIds = matchList.map(_.matchId)
          origSender ! Result(matchIds)
      }
  }

  def failureHandler(origSender: ActorRef): PartialFunction[Throwable, Unit] = {
    case e: Exception =>
      log.error(s"GetRecentMatchIDS request failed for reason: $e")
  }

  private def transform(riotResult: JsObject): List[Match] = {
    val firstKey = riotResult.fields.keys.head
    riotResult.fields(firstKey).convertTo[List[Match]]
  }
}
