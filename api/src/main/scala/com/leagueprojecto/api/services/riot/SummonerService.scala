package com.leagueprojecto.api.services.riot

import akka.actor.Status.Failure
import akka.actor.{ActorLogging, ActorRef, Props, Actor}
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.leagueprojecto.api.JsonProtocols
import com.leagueprojecto.api.domain.Summoner
import com.leagueprojecto.api.services.riot.RiotService.{TooManyRequests, ServiceNotAvailable}
import spray.json._

object SummonerService {
  case object GetSummonerByName
  class SummonerNotFound(message: String) extends Exception

  def props(region: String, name: String): Props = Props(new SummonerService(region, name))
}

class SummonerService(regionParam: String, name: String) extends Actor with ActorLogging with RiotService with JsonProtocols {
  import SummonerService._

  override val region = regionParam
  override val service = summonerByName + name

  override def receive: Receive = {
    case GetSummonerByName =>
      val origSender: ActorRef = sender()

      val summonerEndpoint: Uri = endpoint()

      val future = riotRequest(RequestBuilding.Get(summonerEndpoint))
      future onSuccess {
        case HttpResponse(OK, _, entity, _) =>
          Unmarshal(entity).to[String].onSuccess {
            case result: String =>
              val summoner = transform(result.parseJson.asJsObject)
              origSender ! summoner
          }

        case HttpResponse(NotFound, _, _, _) =>
          val message = s"No summoner found by name '$name' for region '$region'"
          log.warning(message)
          origSender ! Failure(new SummonerNotFound(message))

        case HttpResponse(TooManyRequests, _, _, _) =>
          val message = "Too many requests"
          log.warning(message)
          origSender ! Failure(new TooManyRequests(message))

        case HttpResponse(ServiceUnavailable, _, _, _) =>
          val message = "SummonerService not available"
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

  private def transform(riotResult: JsObject): Summoner = {
    val firstKey = riotResult.fields.keys.head
    riotResult.fields.get(firstKey).get.convertTo[Summoner]
  }
}
