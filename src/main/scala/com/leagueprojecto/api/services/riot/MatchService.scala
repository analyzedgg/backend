package com.leagueprojecto.api.services.riot

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.leagueprojecto.api.domain._
import com.leagueprojecto.api.services.riot.SummonerService.SummonerNotFound
import org.json4s.native.JsonMethods._
import org.json4s._
import org.json4s.native.Serialization

object MatchService {

  case class GetMatch(regionParam: String, summonerId: Long, matchId: Long)
  case class MatchRetrievalFailed(matchId: Long) extends Exception

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
      future onSuccess successHandler(origSender, summonerId, matchId).orElse(defaultSuccessHandler(origSender))
      future onFailure failureHandler
  }

  def successHandler(origSender: ActorRef, summonerId: Long, matchId: Long): PartialFunction[HttpResponse, Unit] = {
    case HttpResponse(OK, _, entity, _) =>
      Unmarshal(entity).to[String].onSuccess {
        case result: String =>
          val matchDetails = transform(result)
          log.debug(s"got match back: $matchDetails")
          val singleMatch: MatchDetail = matchDetails.filter(_.summonerId == summonerId).head
          origSender ! Result(singleMatch)
      }

    case HttpResponse(StatusCodes.TooManyRequests, _, _, _) =>
      log.warning("We're sending too many requests!")
      origSender ! MatchRetrievalFailed(matchId)

    case HttpResponse(NotFound, _, _, _) =>
      val message = s"No match found by service '$service' for region '$region'"
      log.warning(message)
      origSender ! MatchRetrievalFailed(matchId)
  }

  def failureHandler: PartialFunction[Throwable, Unit] = {
    case e: Exception =>
      log.error(s"GetMatch request failed for reason: $e")
  }

  private def transform(riotResult: String): List[MatchDetail] = {
    implicit val formats = Serialization.formats(NoTypeHints)

    val matchObject = parse(riotResult)

    val (matchId, queueType, matchDuration, matchCreation, matchVersion) = (
      (matchObject \ "matchId").extract[Long],
      (matchObject \ "queueType").extract[String],
      (matchObject \ "matchDuration").extract[Int],
      (matchObject \ "matchCreation").extract[Long],
      (matchObject \ "matchVersion").extract[String]
      )

    val participantIds = (matchObject \ "participantIdentities").children.map(pId =>
      ((pId \ "participantId").extract[Int], (pId \ "player" \ "summonerId").extract[Long])
    ).toMap

    val blue: List[Player] = getPlayersFromTeam(matchObject, 100)
    val red: List[Player] = getPlayersFromTeam(matchObject, 200)

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
        matchVersion,
        PlayerStats(
          (stats \ "minionsKilled").extract[Int],
          (stats \ "kills").extract[Int],
          (stats \ "deaths").extract[Int],
          (stats \ "assists").extract[Int]
        ),
        Teams(Team(blue), Team(red))
      )
    })
  }

  private[this] def getPlayersFromTeam(matchObject: JValue, teamId: Int)(implicit formats: Formats): List[Player] = {
    // Create a list of all participants in `teamId`
    val participantIdsInTeam: List[Long] = (matchObject \ "participants").children.map(p =>
      (p \ "participantId").extract[Long] -> (p \ "teamId").extract[Int]
    ).filter(_._2 == teamId).map(_._1)

    // Get the id and name of all above summoners
    (matchObject \ "participantIdentities").children.flatMap(pId => {
      val participantId = (pId \ "participantId").extract[Long]

      if (participantIdsInTeam.contains(participantId)) {
        Some(Player(
          (pId \ "player" \ "summonerId").extract[Long],
          (pId \ "player" \ "summonerName").extract[String])
        )
      } else {
        None
      }
    })
  }
}

