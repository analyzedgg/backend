package com.leagueprojecto.api.services.riot

import akka.actor.Status.Failure
import akka.actor.{ActorLogging, ActorRef, Props, Actor}
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpResponse, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.fasterxml.jackson.databind.ObjectMapper
import com.leagueprojecto.api.domain.{PlayerStats, MatchHistory}
import com.leagueprojecto.api.services.riot.RiotService.{TooManyRequests, ServiceNotAvailable}
import io.gatling.jsonpath.JsonPath

object MatchHistoryService {
  case class GetMatchHistory(region: String, summonerId: Long, beginIndex: Int = 0, endIndex: Int = 15)
  class MatchNotFound(message: String) extends Exception

  def props: Props = Props[MatchHistoryService]
}

class MatchHistoryService extends Actor with ActorLogging with RiotService {
  import MatchHistoryService._

  override def receive: Receive = {
    case GetMatchHistory(region, summonerId, beginIndex, endIndex) =>
      val origSender: ActorRef = sender()

      val queryParams = Map("beginIndex" -> beginIndex.toString,
                            "endIndex" -> endIndex.toString)
      val matchEndpoint: Uri = endpoint(region, matchHistoryBySummonerId + summonerId, queryParams)

      val future = riotRequest(RequestBuilding.Get(matchEndpoint))
      future onSuccess {
        case HttpResponse(OK, _, entity, _) =>
          Unmarshal(entity).to[String].onSuccess {
            case result: String =>
              val matches = transform(result)
              origSender ! matches
          }

        case HttpResponse(NotFound, _, _, _) =>
          val message = s"No games found for summoner id '$summonerId' for region '$region'"
          log.warning(message)
          origSender ! Failure(new MatchNotFound(message))

        case HttpResponse(TooManyRequests, _, _, _) =>
          val message = "Too many requests"
          log.warning(message)
          origSender ! Failure(new TooManyRequests(message))

        case HttpResponse(ServiceUnavailable, _, _, _) =>
          val message = "MatchHistoryService not available"
          log.warning(message)
          origSender ! Failure(new ServiceNotAvailable(message))

        case HttpResponse(status, _, _, _) =>
          val message = s"Something went wrong. API call error code: ${status.intValue()}"
          log.warning(message)
          origSender ! Failure(new IllegalStateException(message))
      }
      future onFailure {
        case e: Exception =>
          println(s"request failed for some reason: ${e.getMessage}")
          e.printStackTrace()
      }
  }

  private def transform(riotResult: String): List[MatchHistory] = {
    val jsonObject = (new ObjectMapper).readValue(riotResult, classOf[Object])

    val matches = JsonPath.query("$.matches[*]['queueType','matchDuration']", jsonObject).right.get.grouped(2).toList
    val stats = JsonPath.query("$.matches[*]['participants'][0]['stats']['minionsKilled']", jsonObject).right.get.toList

    (matches zip stats).map {
      case x =>
        val queueType =     x._1.head.toString
        val matchDuration = x._1(1).toString.toInt
        val minionsKilled = x._2.toString.toInt

        MatchHistory(queueType, matchDuration, PlayerStats(minionsKilled))
    }
  }
}

