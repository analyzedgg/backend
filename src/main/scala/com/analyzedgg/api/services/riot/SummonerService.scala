package com.analyzedgg.api.services.riot

import akka.pattern.pipe
import akka.actor.{ActorLogging, ActorRef, FSM, Props}
import akka.http.scaladsl.model._
import com.analyzedgg.api.domain.Summoner
import com.analyzedgg.api.domain.riot.RiotSummoner
import com.analyzedgg.api.services.riot.SummonerService._

object SummonerService {
  sealed trait State
  case object Idle extends State
  case object WaitingForRiotResponse extends State
  case object RiotRequestFinished extends State

  sealed trait Data
  case object Empty extends Data
  case class RequestData(origin: ActorRef, region: String, name: String) extends Data

  case class GetSummonerByName(region: String, name: String)
  case object SummonerNotFound extends Exception
  case class Result(summoner: Summoner)

  def props = Props(new SummonerService)
}

class SummonerService extends FSM[State, Data] with ActorLogging with RiotService {

  startWith(Idle, Empty)

  when(Idle) {
    case Event(GetSummonerByName(regionParam: String, name: String), Empty) =>
      val origSender: ActorRef = sender()

      riotGetRequest(regionParam, summonerByName + name).pipeTo(self)
      goto(WaitingForRiotResponse) using RequestData(origSender, regionParam, name)
  }

  when(WaitingForRiotResponse) {
    case Event(HttpResponse(StatusCodes.OK, _, entity, _), data: RequestData) =>
      mapRiotTo(entity, classOf[RiotSummoner]).pipeTo(self)
      goto(RiotRequestFinished) using data
    case Event(x, RequestData(origSender, region, name)) =>
      log.warning(s"No summoner found by name $name in region '$region'")
      origSender ! SummonerNotFound
      stop()
    case Event(x, _) =>
      log.error(s"Something went wrong retrieving matches: $x")
      stop()
  }

  when(RiotRequestFinished) {
    case Event(riotSummoner: RiotSummoner, RequestData(origSender, region, name)) =>
      log.debug(s"Got summoner back by name $name from region $region: $riotSummoner")
      origSender ! Result(riotSummoner.summoner)
      stop()
  }
}
