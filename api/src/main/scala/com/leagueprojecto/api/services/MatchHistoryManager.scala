package com.leagueprojecto.api.services

import akka.actor.{ActorLogging, FSM, ActorRef, Props}
import com.leagueprojecto.api.domain.MatchDetail
import com.leagueprojecto.api.services.MatchHistoryManager.{StateData, State}
import com.leagueprojecto.api.services.couchdb.DatabaseService
import com.leagueprojecto.api.services.riot.RecentMatchesService
import akka.pattern.ask
import scala.concurrent.ExecutionContextExecutor

object MatchHistoryManager {

  sealed trait State

  case object Idle extends State

  case object RetrievingRecentMatchIdsFromRiot extends State

  case object RetrievingFromDb extends State

  case object RetrievingFromRiot extends State

  case object PersistingToDb extends State

  case class GetMatches(region: String, summonerId: Long, queueType: String, championList: String)

  case class RequestData(sender: ActorRef, getMatches: GetMatches)

  case class StateData(requestData: Option[RequestData], matches: Map[Long, Option[MatchDetail]])

  def props = Props[MatchHistoryManager]
}

class MatchHistoryManager extends FSM[State, StateData] with ActorLogging {

  import MatchHistoryManager._

  val dbService = context.actorOf(DatabaseService.props)

  startWith(Idle, StateData(None, Map.empty))

  when(Idle) {
    case Event(msg: GetMatches, state) =>
      goto(RetrievingRecentMatchIdsFromRiot) using state.copy(requestData = Some(RequestData(sender(), msg)))
  }

  when(RetrievingRecentMatchIdsFromRiot) {
    case Event(matchIds: Seq[Long], StateData(Some(RequestData(sender, _)), _)) if matchIds.isEmpty =>
      // In case there are no match ids, return an empty MatchHistory Seq back to the sender.
      sender ! Seq.empty[MatchDetail]
      stop()
    case Event(matchIds: Seq[Long], state) =>
      // create the empty matchesMap which are to be filled by either the Db or Riot
      val matchesMap = matchIds.map(m => m -> None).toMap
      log.info(s"Match ids from rito $matchesMap")
      goto(RetrievingFromDb) using state.copy(matches = matchesMap)

    // todo: handle riot errors
  }

  when(RetrievingFromDb) {
    case Event(matchHistories: Seq[MatchDetail], StateData(Some(RequestData(sender, msg)), matches)) =>
      val mergedMatches = matches ++ matchHistories.map(m => m.matchId -> Some(m))

      if (!hasEmptyValues(mergedMatches)) {
        sender ! getValues(mergedMatches)
        stop()
      } else {
        goto(RetrievingFromRiot) using StateData(Some(RequestData(sender, msg)), mergedMatches)
      }
  }

  when(RetrievingFromRiot) {
    case Event(matchHistories: Seq[MatchDetail], StateData(Some(RequestData(sender, msg)), matches)) =>
      val mergedMatches = matches ++ matchHistories.map(m => m.matchId -> Some(m))

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
      nextStateData match {
        case StateData(Some(RequestData(_, GetMatches(region, summonerId, queueType, championList))), _) =>
          log.info("Requesting last 10 match ids from Riot")
          val recentMatchesTest = context.actorOf(RecentMatchesService.props(region, summonerId, queueType, championList))
          recentMatchesTest ! RecentMatchesService.GetRecentMatchIds(10)
        case failData =>
          log.error(s"Something went wrong when going from Idle -> RetrievingRecentMatchIdsFromRiot, got data: $failData")
      }
    case RetrievingRecentMatchIdsFromRiot -> RetrievingFromDb =>
      nextStateData match {
        case StateData(Some(RequestData(_, GetMatches(region, summonerId, queueType, championList))), matches) =>
          dbService ! DatabaseService.GetMatches(region, summonerId, matches.keys.toSeq)
        case failData =>
          log.error(s"Something went wrong when going from RetrievingRecentMatchIdsFromRiot -> RetrievingFromDb, got data: $failData")
      }
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


  private def hasEmptyValues(mergedMatches: Map[Long, Option[MatchDetail]]): Boolean = {
    mergedMatches.exists(_._2.isEmpty)
  }

  private def getValues(mergedMatches: Map[Long, Option[MatchDetail]]): Seq[MatchDetail] = {
    mergedMatches.values.map(_.get).toSeq
  }
}
