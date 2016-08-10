package com.analyzedgg.api.services.riot

import akka.actor.Status.Failure
import akka.pattern.pipe
import akka.actor.{ActorLogging, ActorRef, FSM, Props}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.analyzedgg.api.domain.ChampionList
import com.analyzedgg.api.services.riot.ChampionService._
import spray.json._

object ChampionService {

  sealed trait State

  case object Idle extends State

  case object WaitingForRiotResponse extends State

  case object RiotRequestFinished extends State

  sealed trait Data

  case object Empty extends Data

  case class RequestData(origin: ActorRef) extends Data

  case class GetChampions(region: String)

  case class ChampionsResponse(championList: ChampionList)

  case object FailedRetrievingChampions extends Exception

  def props = Props[ChampionService]
}

class ChampionService extends FSM[State, Data] with ActorLogging with RiotService {

  startWith(Idle, Empty)


  when(Idle) {
    case Event(GetChampions(regionParam: String), Empty) =>
      val origSender: ActorRef = sender()
      riotGetRequest(regionParam, championByTags, Map(("champData", "tags")), "api/lol/static-data", "global").pipeTo(self)
      goto(WaitingForRiotResponse) using RequestData(origSender)
  }

  when(WaitingForRiotResponse) {
    case Event(HttpResponse(StatusCodes.OK, _, entity, _), data: RequestData) =>
      mapRiotTo(entity, classOf[ChampionList]).pipeTo(self)
      goto(RiotRequestFinished) using data
    case Event(x, RequestData(origSender)) =>
      log.error(s"Something went wrong retrieving champions: $x")
      origSender ! Failure(FailedRetrievingChampions)
      stop()
  }

  when(RiotRequestFinished) {
    case Event(championList: ChampionList, RequestData(origSender)) =>
      log.debug(s"Got match back: $championList")
      origSender ! ChampionsResponse(championList)
      stop()
  }
}