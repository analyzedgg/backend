package com.leagueprojecto.api.services

import akka.actor.{ActorLogging, FSM, ActorRef, Props}
import com.leagueprojecto.api.domain.MatchHistory
import com.leagueprojecto.api.services.MatchHistoryManager.{StateData, State}

object MatchHistoryManager {

  sealed trait State
  case object Idle extends State
  case object RetrievingRecentMatchIdsFromRiot extends State
  case object RetrievingFromDb extends State
  case object RetrievingFromRiot extends State
  case object PersistingToDb extends State

  case class GetMatches(region: String, summonerId: Long, queueType: String, championList: String)
  case class RequestData(sender: ActorRef, getMatches: GetMatches)
  
  case class StateData(requestData: Option[RequestData], matches: Map[Long, Option[MatchHistory]])

  def props = Props[MatchHistoryManager]
}
class MatchHistoryManager extends FSM[State, StateData] with ActorLogging {
  import MatchHistoryManager._
  
  startWith(Idle, StateData(None, Map.empty))
  
  when(Idle) {
    case Event(msg: GetMatches, state) =>
      goto(RetrievingRecentMatchIdsFromRiot) using state.copy(requestData = Some(RequestData(sender(), msg)))
  }

  when(RetrievingRecentMatchIdsFromRiot) {
    case Event(matchIds: Seq[Long], StateData(Some(RequestData(sender, _)), _)) if matchIds.isEmpty =>
      sender ! Seq.empty[MatchHistory]
      stop()
    case Event(matchIds: Seq[Long], state) =>
      // create the empty matchesMap which are to be filled by either the Db or Riot
      val matchesMap = matchIds.map(m => m -> None).toMap
      goto(RetrievingFromDb) using state.copy(matches = matchesMap)




    // todo: handle riot errors
  }

  when(RetrievingFromDb) {
    case Event(matchHistories: Map[Long, MatchHistory], StateData(Some(RequestData(sender, msg)), matches)) =>
      val mergedMatches = matches ++ matchHistories.mapValues(v => Some(v))
      if (!hasEmptyValues(mergedMatches)) {
        sender ! getValues(mergedMatches)
        stop()
      } else {
        goto(RetrievingFromRiot) using StateData(Some(RequestData(sender, msg)), mergedMatches)
      }
  }

  when(RetrievingFromRiot) {
    case Event(matchHistories: Map[Long, MatchHistory], StateData(Some(RequestData(sender, msg)), matches)) =>
      val mergedMatches = matches ++ matchHistories.mapValues(v => Some(v))

      // todo: write `matchHistories` to db, would be nicer in the transition, but how can we distinguish new matches from Riot there?

      if (!hasEmptyValues(mergedMatches)) {
        sender ! mergedMatches
        stop()
      } else {
        log.error("Got response back from Riot, but matchHistories is still not fully filled.")
        stop()
      }


      // todo: handle riot errors
  }

  when(PersistingToDb) {
    //case Event(MatchHistoriesSaved, _) => stop()
    case _ => stop()
  }

  onTransition {
    case Idle -> RetrievingRecentMatchIdsFromRiot =>
      log.info("g o  f i x")
    case RetrievingRecentMatchIdsFromRiot -> RetrievingFromDb =>
      log.info("g o  f i x")
    case RetrievingFromDb -> RetrievingFromRiot =>
      log.info("g o  f i x")
    case RetrievingFromRiot -> PersistingToDb =>
      log.info("g o  f i x")
  }

  whenUnhandled {
    case Event(msg, _) =>
      log.error(s"Got unhandled message ($msg) in $stateName")
      stop()
  }


  def hasEmptyValues(mergedMatches: Map[Long, Option[MatchHistory]]): Boolean = {
    mergedMatches.exists(_._2.isEmpty)
  }

  def getValues(mergedMatches: Map[Long, Option[MatchHistory]]): Seq[MatchHistory] = {
    mergedMatches.values.map(_.get).toSeq
  }
}
