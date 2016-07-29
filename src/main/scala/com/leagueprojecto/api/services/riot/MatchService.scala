package com.leagueprojecto.api.services.riot

import akka.actor.{ActorLogging, ActorRef, FSM, Props}
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.{HttpResponse, StatusCodes, Uri}
import akka.pattern.pipe
import com.leagueprojecto.api.domain._
import com.leagueprojecto.api.services.riot.MatchService._

object MatchService {

  sealed trait State
  case object Idle extends State
  case object WaitingForRiotResponse extends State
  case object RiotRequestFinished extends State

  sealed trait Data
  case object Empty extends Data
  case class RequestData(origin: ActorRef, matchId: Long, summerId: Long) extends Data
  case class GetMatch(regionParam: String, summonerId: Long, matchId: Long)

  case class MatchRetrievalFailed(matchId: Long) extends Exception

  case class Result(riotMatch: RiotMatch)

  def props = Props(new MatchService)
}

class MatchService extends FSM[State, Data] with ActorLogging with RiotService {

  startWith(Idle, Empty)

  when(Idle) {
    case Event(GetMatch(regionParam, summonerId, matchId), Empty) =>
      val origSender: ActorRef = sender()

      val matchEndpoint: Uri = endpoint(regionParam, matchById + matchId)
      riotRequest(RequestBuilding.Get(matchEndpoint)).pipeTo(self)

      goto(WaitingForRiotResponse) using RequestData(origSender, summonerId, matchId)
  }

  when(WaitingForRiotResponse) {
    case Event(msg@HttpResponse(StatusCodes.OK, _, entity, _), data: RequestData) =>
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
      log.debug(s"got match back: $matchDetails")
      val givenParticipantsIdentity = matchDetails.participantIdentities.filter(_.player.summonerId == summonerId)
      val givenParticipantData = matchDetails.participants.filter(_.participantId == givenParticipantsIdentity.head.participantId)
      val singleMatch: RiotMatch = matchDetails.copy(participantIdentities = givenParticipantsIdentity, participants = givenParticipantData)
      log.debug(s"returning single match for $summonerId: $singleMatch")
      origSender ! Result(singleMatch)
      stop()
  }
}

