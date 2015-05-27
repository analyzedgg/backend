package com.leagueprojecto.api.services.riot

import akka.actor.Status.Failure
import akka.actor.{ActorLogging, ActorRef, Props, Actor}
import com.leagueprojecto.api.JsonProtocols
import com.leagueprojecto.api.domain.Summoner
import com.leagueprojecto.api.services.riot.RiotService.ServiceNotAvailable
import com.ning.http.client.{Response, AsyncCompletionHandler}
import spray.json._

object SummonerService {
  case class GetSummonerByName(region: String, name: String)
  class SummonerNotFound(message: String) extends Exception

  def props: Props = Props[SummonerService]
}

class SummonerService extends Actor with ActorLogging with RiotService with JsonProtocols {
  import SummonerService._

  override def receive: Receive = {
    case GetSummonerByName(region, name) =>
      val origSender: ActorRef = sender()

      httpClient.prepareGet(riotApi(region, summoner_byname + name))
        .addQueryParam("api_key", api_key)
        .execute(new AsyncCompletionHandler[Response]() {
        override def onCompleted(response: Response): Response = response.getStatusCode match {
          case 200 =>
            val result = response
              .getResponseBody
              .parseJson
              .asJsObject

            val summoner = transform(result)
            origSender ! summoner
            response // TODO: Do we always need to return something?

          case 404 =>
            val message = s"No summoner found by name '$name' for region '$region'"
            log.warning(message)
            origSender ! Failure(new SummonerNotFound(message))
            response

          case 503 =>
            val message = s"SummonerService not available"
            log.warning(message)
            origSender ! Failure(new ServiceNotAvailable(message))
            response

          case code: Int =>
            val message = s"Something went wrong. API call error code: $code"
            log.warning(message)
            origSender ! Failure(new IllegalStateException(message))
            response
        }
      })
  }

  private def transform(riotResult: JsObject): Summoner = {
    val firstKey = riotResult.fields.keys.head
    riotResult.fields.get(firstKey).get.convertTo[Summoner]
  }
}
