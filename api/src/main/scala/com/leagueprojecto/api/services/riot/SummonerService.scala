package com.leagueprojecto.api.services.riot

import akka.actor.{ActorRef, Props, Actor}
import com.leagueprojecto.api.JsonProtocols
import com.leagueprojecto.api.domain.Summoner
import com.ning.http.client.{Response, AsyncCompletionHandler}
import Summoner
import spray.json._

object SummonerService {
  case class GetSummonerByName(region: String, name: String)

  def props: Props = Props[SummonerService]
}

class SummonerService extends Actor with RiotService with JsonProtocols {
  import SummonerService._

  override def receive: Receive = {
    case GetSummonerByName(region ,name) =>
      val origSender: ActorRef = sender()

      httpClient.prepareGet(riotApi(region, summoner_byname + name))
        .addQueryParam("api_key", api_key)
        .execute(new AsyncCompletionHandler[Response]() {
        override def onCompleted(response: Response): Response = {
          val result = response
            .getResponseBody
            .parseJson
            .asJsObject

          val firstKey = result.fields.keys.head
          val summoner = result.fields.get(firstKey).get.convertTo[Summoner]

          origSender ! summoner

          response
        }
      })

  }
}
