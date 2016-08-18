package com.analyzedgg.api.services.riot

import akka.actor.{ActorLogging, ActorRef, FSM, Props}
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.{HttpResponse, StatusCodes, Uri}
import akka.pattern.pipe
import com.analyzedgg.api.domain._
import com.analyzedgg.api.domain.riot._
import com.analyzedgg.api.services.riot.MatchService._

object MatchService {

  sealed trait State
  case object Idle extends State
  case object WaitingForRiotResponse extends State
  case object RiotRequestFinished extends State

  sealed trait Data
  case object Empty extends Data
  case class RequestData(origin: ActorRef, summonerId: Long, matchId: Long) extends Data

  case class GetMatch(regionParam: String, summonerId: Long, matchId: Long)
  case class MatchRetrievalFailed(matchId: Long) extends Exception
  case class Result(matchDetail: MatchDetail)

  def props = Props(new MatchService)
}

class MatchService extends FSM[State, Data] with ActorLogging with RiotService {

  startWith(Idle, Empty)

  when(Idle) {
    case Event(GetMatch(regionParam, summonerId, matchId), Empty) =>
      val origSender: ActorRef = sender()
      riotGetRequest(regionParam, matchById + matchId).pipeTo(self)

      goto(WaitingForRiotResponse) using RequestData(origSender, summonerId, matchId)
  }

  when(WaitingForRiotResponse) {
    case Event(HttpResponse(StatusCodes.OK, _, entity, _), data: RequestData) =>
      mapRiotTo(entity, classOf[RiotMatch]).pipeTo(self)
      goto(RiotRequestFinished) using data
    case Event(x, RequestData(origSender, _, matchId)) =>
      log.error(s"Something went wrong retrieving matches: $x")
      origSender ! MatchRetrievalFailed(matchId)
      stop()
  }

  // TODO: Now we filter the information of one person, but we could return all the user data to save in the db, would be more data on the frontend as counterpoint
  when(RiotRequestFinished) {
    case Event(matchDetails: RiotMatch, RequestData(origSender, summonerId, matchId)) =>
      log.debug(s"Got match back: $matchDetails")

      val responderMatch = toMatchDetail(matchDetails, summonerId)
      log.debug(s"Got the match back for $summonerId: $responderMatch")
      origSender ! Result(responderMatch)
      stop()
  }


  private[this] def toMatchDetail(riotMatch: RiotMatch, summonerId: Long): MatchDetail = {
    val participantIdentity = riotMatch.participantIdentities.filter(_.player.summonerId == summonerId).head
    val participant = riotMatch.participants.filter(_.participantId == participantIdentity.participantId).head

    val blue: Seq[Player] = getPlayersFromTeam(riotMatch.participants, riotMatch.participantIdentities, 100)
    val red: Seq[Player] = getPlayersFromTeam(riotMatch.participants, riotMatch.participantIdentities, 200)

    MatchDetail(
      riotMatch.matchId,
      riotMatch.queueType,
      riotMatch.matchDuration,
      riotMatch.matchCreation,
      participantIdentity.player.summonerId,
      participant.championId,
      participant.timeline.role,
      participant.timeline.lane,
      participant.stats.winner,
      riotMatch.matchVersion,
      toPlayerStats(participant.stats),
      Teams(Team(blue), Team(red))
    )
  }

  private[this] def getPlayersFromTeam(participants: Seq[Participant], participantIdentities: Seq[ParticipantIdentity], teamId: Int): Seq[Player] = {
    // Create a list of all participants in `teamId`
    val participantIdsInTeam: Seq[Long] = participants.map(p =>
      p.participantId -> p.teamId
    ).filter(_._2 == teamId).map(_._1)

    // Get the id and name of all above summoners
    participantIdentities.flatMap(pId => {
      val participantId = pId.participantId

      if (participantIdsInTeam.contains(participantId)) {
        Some(
          Player(
            pId.player.summonerId,
            pId.player.summonerName
          )
        )
      } else {
        None
      }
    })
  }

  private[this] def toPlayerStats(stats: ParticipantStats): PlayerStats = {
    PlayerStats(stats.minionsKilled, stats.kills, stats.deaths, stats.assists)
  }
}

