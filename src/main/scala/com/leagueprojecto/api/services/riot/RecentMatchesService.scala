package com.leagueprojecto.api.services.riot

import akka.actor.{ActorLogging, ActorRef, FSM, Props}
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.{HttpResponse, StatusCodes, Uri}
import akka.pattern.pipe
import com.leagueprojecto.api.domain.riot.RiotRecentMatches
import com.leagueprojecto.api.services.riot.RecentMatchesService._

object RecentMatchesService {
  sealed trait State
  case object Idle extends State
  case object WaitingForRiotResponse extends State
  case object RiotRequestFinished extends State

  sealed trait Data
  case object Empty extends Data
  case class RequestData(origin: ActorRef, summonerId: Long) extends Data

  case object FailedRetrievingRecentMatches extends Exception

  case class GetRecentMatchIds(region: String, summonerId: Long, queueType: String, championList: String, amount: Int)
  case class Result(matchIds: Seq[Long])

  def props = Props(new RecentMatchesService)
}

class RecentMatchesService extends FSM[State, Data] with RiotService with ActorLogging {

  startWith(Idle, Empty)

  when(Idle) {
    case Event(GetRecentMatchIds(regionParam, summonerId, queueType, championList, amount), Empty) =>
      val origSender = sender()

      val queryParams: Map[String, String] = Map("beginIndex" -> (0 toString), "endIndex" -> (amount toString))

      riotGetRequest(regionParam, matchListBySummonerId + summonerId, queryParams).pipeTo(self)
      goto(WaitingForRiotResponse) using RequestData(origSender, summonerId)
  }

  when(WaitingForRiotResponse) {
    case Event(HttpResponse(StatusCodes.OK, _, entity, _), data: RequestData) =>
      mapRiotTo(entity, classOf[RiotRecentMatches]).pipeTo(self)
      goto(RiotRequestFinished) using data

    case Event(x, RequestData(origSender, summonerId)) =>
      log.error(s"GetRecentMatchIDS request failed for reason: $x")
      origSender ! FailedRetrievingRecentMatches
      stop()
  }

  when(RiotRequestFinished) {
    case Event(riotRecentMatches: RiotRecentMatches, RequestData(origSender, summonerId)) =>
      log.debug(s"Got recent matchIds back: $riotRecentMatches")
      val matchIds = riotRecentMatches.matches.map(_.matchId)
      origSender ! Result(matchIds)
      stop()
  }
}
