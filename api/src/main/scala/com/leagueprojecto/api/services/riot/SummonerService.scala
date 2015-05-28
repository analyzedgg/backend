package com.leagueprojecto.api.services.riot

import akka.actor.Status.Failure
import akka.actor.{ActorLogging, ActorRef, Props, Actor}
import com.leagueprojecto.api.JsonProtocols
import com.leagueprojecto.api.domain.Summoner
import com.leagueprojecto.api.services.riot.RiotService.{TooManyRequests, ServiceNotAvailable}
import com.ning.http.client.{ListenableFuture, Response}
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

      val future: ListenableFuture[Response] = httpClient.prepareGet(riotApi(region, summoner_byname + name))
        .addQueryParam("api_key", api_key)
        .execute()

      future.addListener(new Runnable {
        override def run(): Unit = {
          val response = future.get()

          response.getStatusCode match {
            case 200 =>
              val result = response
                .getResponseBody
                .parseJson
                .asJsObject

              val summoner = transform(result)
              origSender ! summoner

            case 404 =>
              val message = s"No summoner found by name '$name' for region '$region'"
              log.warning(message)
              origSender ! Failure(new SummonerNotFound(message))

            case 429 =>
              val message = s"Too many requests"
              log.warning(message)
              origSender ! Failure(new TooManyRequests(message))

            case 503 =>
              val message = s"SummonerService not available"
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

  private def transform(riotResult: JsObject): Summoner = {
    val firstKey = riotResult.fields.keys.head
    riotResult.fields.get(firstKey).get.convertTo[Summoner]
  }
}
