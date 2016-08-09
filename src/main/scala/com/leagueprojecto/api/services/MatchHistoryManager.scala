package com.leagueprojecto.api.services

import akka.actor.{ActorLogging, ActorRef, FSM, Props}
import akka.actor.Status.Failure
import akka.pattern.CircuitBreaker
import com.leagueprojecto.api.domain.MatchDetail
import com.leagueprojecto.api.domain.riot.RiotRecentMatches
import com.leagueprojecto.api.services.MatchHistoryManager.{State, StateData}
import com.leagueprojecto.api.services.couchdb.DatabaseService
import com.leagueprojecto.api.services.couchdb.DatabaseService.{MatchesResult, MatchesSaved, NoResult, SaveMatches}
import com.leagueprojecto.api.services.riot.RecentMatchesService

import scala.concurrent.duration._

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

  case class Result(data: Seq[MatchDetail])

  def props(couchDbCircuitBreaker: CircuitBreaker) = Props(new MatchHistoryManager(couchDbCircuitBreaker))
}

class MatchHistoryManager(couchDbCircuitBreaker: CircuitBreaker) extends FSM[State, StateData] with ActorLogging {

  import MatchHistoryManager._

  val dbService = context.actorOf(DatabaseService.props(couchDbCircuitBreaker), "dbService")

  startWith(Idle, StateData(None, Map.empty))

  when(Idle) {
    case Event(msg: GetMatches, state) =>
      goto(RetrievingRecentMatchIdsFromRiot) using state.copy(requestData = Some(RequestData(sender(), msg)))
  }

  when(RetrievingRecentMatchIdsFromRiot) {
    case Event(RecentMatchesService.Result(matchIds), StateData(Some(RequestData(sender, _)), _)) if matchIds.isEmpty =>
      // In case there are no match ids, return an empty MatchHistory Seq back to the sender.
      sender ! Result(Seq.empty[MatchDetail])
      stop()
    case Event(RecentMatchesService.Result(matchIds), state) =>
      // create the empty matchesMap which are to be filled by either the Db or Riot
      val matchesMap = matchIds.map(m => m -> None).toMap
      goto(RetrievingFromDb) using state.copy(matches = matchesMap)
    case Event(e @ RecentMatchesService.FailedRetrievingRecentMatches, StateData(Some(RequestData(sender, _)), _)) =>
      sender ! Failure(e)
      stop()
  }

  when(RetrievingFromDb) {
    case Event(MatchesResult(matchDetails), StateData(Some(RequestData(sender, msg)), matches)) =>
      val mergedMatches = matches ++ matchDetails.map(m => m.matchId -> Some(m))

      if (!hasEmptyValues(mergedMatches)) {
        log.debug("Returning matches from RIOT")
        sender ! Result(getValues(mergedMatches).sortBy(_.matchId))
        stop()
      } else {
        goto(RetrievingFromRiot) using StateData(Some(RequestData(sender, msg)), mergedMatches)
      }

    case Event(NoResult, StateData(Some(RequestData(sender, msg)), matches)) =>
      goto(RetrievingFromRiot) using StateData(Some(RequestData(sender, msg)), matches)
  }

  when(RetrievingFromRiot) {
    case Event(MatchCombiner.Result(matchDetails), StateData(Some(RequestData(sender, msg@GetMatches(region, summonerId, _, _))), matches)) =>

      // Insert the new matchDetails into the database
      dbService ! SaveMatches(region, summonerId, matchDetails)

      val mergedMatches = matches ++ matchDetails.map(m => m.matchId -> Some(m))
      val mergedMatchesSeq = mergedMatches.values.flatten.toSeq
      sender ! Result(mergedMatchesSeq.sortBy(_.matchId))
      log.debug("Returning merged matches from RIOT")
      goto(PersistingToDb) using StateData(Some(RequestData(sender, msg)), mergedMatches)
  }

  when(PersistingToDb, stateTimeout = 10.seconds) {
    case Event(MatchesSaved, _) =>
      log.info("Matches were saved correctly, stopping actor")
      stop()
    case _ =>
      stop()
  }

  onTransition {
    case Idle -> RetrievingRecentMatchIdsFromRiot =>
      nextStateData match {
        case StateData(Some(RequestData(_, GetMatches(region, summonerId, queueType, championList))), _) =>
          log.info("Requesting last 20 match ids from Riot")
          val recentMatchesActor = context.actorOf(RecentMatchesService.props)
          recentMatchesActor ! RecentMatchesService.GetRecentMatchIds(region, summonerId, queueType, championList, 20)
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
      nextStateData match {
        case StateData(Some(RequestData(_, GetMatches(region, summonerId, _, _))), matches) =>
          val matchesToRetrieve = matches.filter(_._2.isEmpty).keys
          log.info(s"going to get matches: [$matchesToRetrieve] from riot")
          val matchesActor = context.actorOf(MatchCombiner.props, "matchesActor")
          matchesActor ! MatchCombiner.GetMatches(region, summonerId, matchesToRetrieve.toSeq)
      }
  }

  whenUnhandled {
    case Event(msg, StateData(Some(RequestData(sender, _)), _)) =>
      log.error(s"Got unhandled message ($msg) in $stateName")
      sender ! Failure(new IllegalStateException())
      stop()
  }

  private def hasEmptyValues(mergedMatches: Map[Long, Option[MatchDetail]]): Boolean =
    mergedMatches.values.exists(_.isEmpty)

  private def getValues(mergedMatches: Map[Long, Option[MatchDetail]]): Seq[MatchDetail] =
    mergedMatches.values.map(_.get).toSeq
}
