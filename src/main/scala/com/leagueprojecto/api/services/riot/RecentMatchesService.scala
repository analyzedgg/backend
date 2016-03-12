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
  case class GetRecentMatchIds(amount: Int)
  case class Result(matchIds: Seq[Long])

  def props(region: String, summonerId: Long, queueType: String, championList: String) =
    Props(new RecentMatchesService(region, summonerId, queueType, championList))
}

class RecentMatchesService(override val region: String, summonerId: Long, queueType: String, championList: String) extends Actor
with RiotService with ActorLogging with JsonProtocols {
  import RecentMatchesService._

  override val service = matchlistBySummonerId + summonerId

  override def receive: Receive = {
    case GetRecentMatchIds(amount) =>
      implicit val origSender = sender()

      // riot service was giving 500 at the time for begin and end index 0 and 100 while it worked at first
      val queryParams = Map.empty[String, String]

      val matchlistEndpoint: Uri = endpoint(queryParams)

      val future = riotRequest(RequestBuilding.Get(matchlistEndpoint))
      future onSuccess successHandler(origSender, amount).orElse(defaultSuccessHandler(origSender))
      future onFailure failureHandler(origSender)
  }

  def successHandler(origSender: ActorRef, amount: Int): PartialFunction[HttpResponse, Unit] = {
    case HttpResponse(OK, _, entity, _) =>
      Unmarshal(entity).to[String].onSuccess {
        case result: String =>
          val matchlist = transform(result.parseJson.asJsObject)
          println(s"${matchlist.size} matches found!")
          val matchIds = matchlist.map(_.matchId).take(5).toSeq
          origSender ! Result(matchIds)
      }
  }

  def failureHandler(origSender: ActorRef): PartialFunction[Throwable, Unit] = {
    case e: Exception =>
      log.error(e, s"request failed for some reason")
  }

  private def transform(riotResult: JsObject): List[Match] = {
    val firstKey = riotResult.fields.keys.head
    riotResult.fields.get(firstKey).get.convertTo[List[Match]]
  }
}
