package com.leagueprojecto.api.services.riot

import akka.actor.{Props, ActorRef, Actor}
import com.leagueprojecto.api.JsonProtocols
import com.leagueprojecto.api.domain.MatchHistory
import com.ning.http.client.{Response, AsyncCompletionHandler}
import MatchHistory
import spray.json._

object MatchHistoryService {
  case class GetMatchHistory(region: String, summonerId: Long, endIndex: Int)

  def props: Props = Props[MatchHistoryService]
}

class MatchHistoryService extends Actor with RiotService with JsonProtocols {
  import MatchHistoryService._

  override def receive: Receive = {
    case GetMatchHistory(region, summonerId, endIndex) =>
      val origSender: ActorRef = sender()

      httpClient.prepareGet(riotApi(region, summoner_byname + summonerId))
        .addQueryParam("api_key", api_key)
        .execute(new AsyncCompletionHandler[Response]() {
        override def onCompleted(response: Response): Response = {
          val result = response
            .getResponseBody
            .parseJson
            .asJsObject

          val firstKey = result.fields.keys.head
          val matchHistory = result.fields.get(firstKey).get.convertTo[List[MatchHistory]]

          origSender ! matchHistory

          response
        }
      })

  }
}
