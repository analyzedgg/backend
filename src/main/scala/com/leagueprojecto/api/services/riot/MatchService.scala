package com.leagueprojecto.api.services.riot

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpResponse, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.leagueprojecto.api.domain.{MatchDetail, Player, PlayerStats, Team}
import com.leagueprojecto.api.services.riot.SummonerService.SummonerNotFound
import org.json4s.native.JsonMethods._
import org.json4s._
import org.json4s.native.Serialization

import scala.collection.mutable.ListBuffer

object MatchService {

  case class GetMatch(regionParam: String, summonerId: Long, matchId: Long)

  case class Result(matchDetail: MatchDetail)

  def props = Props(new MatchService)
}

class MatchService extends Actor with ActorLogging with RiotService {

  import MatchService._

  override def receive: Receive = {
    case GetMatch(regionParam, summonerId, matchId) =>
      implicit val origSender: ActorRef = sender()

      val matchEndpoint: Uri = endpoint(regionParam, matchById + matchId)

      val future = riotRequest(RequestBuilding.Get(matchEndpoint))
      future onSuccess successHandler(origSender, summonerId).orElse(defaultSuccessHandler(origSender))
      future onFailure failureHandler
  }

  def successHandler(origSender: ActorRef, summonerId: Long): PartialFunction[HttpResponse, Unit] = {
    case HttpResponse(OK, _, entity, _) =>
      Unmarshal(entity).to[String].onSuccess {
        case result: String =>
          val matchDetails = transform(result)
          log.debug(s"got match back: $matchDetails")
          val singleMatch: MatchDetail = matchDetails.filter(_.summonerId == summonerId).head
          origSender ! Result(singleMatch)
      }

    case HttpResponse(NotFound, _, _, _) =>
      val message = s"No match found by service '$service' for region '$region'"
      log.warning(message)
      origSender ! Failure(new SummonerNotFound(message))
  }

  def failureHandler: PartialFunction[Throwable, Unit] = {
    case e: Exception =>
      println(s"request failed for some reason: ${e.getMessage}")
      e.printStackTrace()
  }

  private def transform(riotResult: String): List[MatchDetail] = {
    implicit val formats = Serialization.formats(NoTypeHints)

    val matchObject = parse(riotResult)

    val (matchId, queueType, matchDuration, matchCreation) = (
      (matchObject \ "matchId").extract[Long],
      (matchObject \ "queueType").extract[String],
      (matchObject \ "matchDuration").extract[Int],
      (matchObject \ "matchCreation").extract[Long]
      )

    val participantIds = (matchObject \ "participantIdentities").children.map(pId =>
      ((pId \ "participantId").extract[Int], (pId \ "player" \ "summonerId").extract[Long])
    ).toMap

    var players = new ListBuffer[Player]()

    (matchObject \ "participantIdentities").children.foreach(pId =>
      players += Player((pId \ "player" \ "summonerId").extract[Long], (pId \ "player" \ "summonerName").extract[String])
    )

    val blue = players.take(5)
    val red = players.takeRight(5)

    val blueList = blue.toList
    val redList = red.toList

    (matchObject \ "participants").children.map(p => {
      val (stats, timeline) = (p \ "stats", p \ "timeline")

      MatchDetail(
        matchId,
        queueType,
        matchDuration,
        matchCreation,
        participantIds((p \ "participantId").extract[Int]),
        (p \ "championId").extract[Int],
        (timeline \ "role").extract[String],
        (timeline \ "lane").extract[String],
        (stats \ "winner").extract[Boolean],
        PlayerStats(
          (stats \ "minionsKilled").extract[Int],
          (stats \ "kills").extract[Int],
          (stats \ "deaths").extract[Int],
          (stats \ "assists").extract[Int]
        ),
        Team(blueList),
        Team(redList)
      )
    }
    )
  }
}

