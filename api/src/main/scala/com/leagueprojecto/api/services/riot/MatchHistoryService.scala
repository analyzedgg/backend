package com.leagueprojecto.api.services.riot

import akka.actor.Status.Failure
import akka.actor.{ActorLogging, ActorRef, Props, Actor}
import com.fasterxml.jackson.databind.ObjectMapper
import com.leagueprojecto.api.domain.{PlayerStats, MatchHistory}
import com.leagueprojecto.api.services.riot.RiotService.{TooManyRequests, ServiceNotAvailable}
import com.ning.http.client.{ListenableFuture, Response}
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

      val future: ListenableFuture[Response] =
        httpClient.prepareGet(riotApi(region, matchHistoryBySummonerId + summonerId))
        .addQueryParam("api_key", api_key)
        .addQueryParam("beginIndex", beginIndex.toString)
        .addQueryParam("endIndex", endIndex.toString)
        .execute()

      future.addListener(new Runnable {
        override def run(): Unit = {
          val response = future.get()

          response.getStatusCode match {
            case 200 =>
              val result = response.getResponseBody
              val matches = transform(result)
              origSender ! matches

            case 404 =>
              val message = s"No games found for summoner id '$summonerId' for region '$region'"
              log.warning(message)
              origSender ! Failure(new MatchNotFound(message))

            case 429 =>
              val message = s"Too many requests"
              log.warning(message)
              origSender ! Failure(new TooManyRequests(message))

            case 503 =>
              val message = s"MatchHistoryService not available"
              log.warning(message)
              origSender ! Failure(new ServiceNotAvailable(message))

            case code: Int =>
              val message = s"Something went wrong. API call error code: $code"
              log.warning(message)
              origSender ! Failure(new IllegalStateException(message))
          }
        }
      }, context.dispatcher)
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

