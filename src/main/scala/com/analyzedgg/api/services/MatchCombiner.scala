package com.analyzedgg.api.services

import akka.actor._
import com.analyzedgg.api.domain.MatchDetail
import com.analyzedgg.api.services.MatchCombiner._
import com.analyzedgg.api.services.riot.MatchService
import com.analyzedgg.api.services.riot.MatchService.GetMatch

import scala.concurrent.duration._

object MatchCombiner {
  case class GetMatches(regionParam: String, summonerId: Long, matchIds: Seq[Long])
  case object TimeOut

  sealed trait State
  case object Idle extends State
  case object GettingMatches extends State

  case class StateData(sender: ActorRef, matches: Map[Long, Option[MatchDetail]])
  case class Result(matches: Seq[MatchDetail])

  def props = Props(new MatchCombiner)
}

class MatchCombiner extends FSM[State, StateData] with ActorLogging {
  startWith(Idle, StateData(Actor.noSender, Map.empty))

  when(Idle) {
    case Event(GetMatches(regionParam, summonerId, matchIds), _) =>
      matchIds.foreach(id => createMatchServiceActor ! GetMatch(regionParam, summonerId, id))
      val matchesMap = matchIds.map(m => m -> None).toMap
      goto(GettingMatches) using StateData(sender(), matchesMap)
  }

  when(GettingMatches, stateTimeout = 5 seconds) {
    case Event(MatchService.Result(matchDetail: MatchDetail), state @ StateData(sender, matches)) =>

      val newMatches = matches + (matchDetail.matchId -> Some(matchDetail))

      if (hasEmptyValues(newMatches)) {
        goto(GettingMatches) using state.copy(matches = newMatches)
      } else {
        sender ! Result(getValuesInOrder(newMatches))
        stop()
      }

    case Event(MatchService.MatchRetrievalFailed(matchId), state @ StateData(sender, matches)) =>

      val newMatches = matches - matchId

      if (hasEmptyValues(newMatches)) {
        goto(GettingMatches) using state.copy(matches = newMatches)
      } else {
        sender ! Result(getValuesInOrder(newMatches))
        stop()
      }

    case Event(StateTimeout, StateData(sender, matches)) =>
      log.info(s"MatchCombiner timed out, returning ${matches.size} matches.")
      sender ! Result(getValuesInOrder(matches))
      stop()
  }

  private def hasEmptyValues(mergedMatches: Map[Long, Option[MatchDetail]]): Boolean =
    mergedMatches.values.exists(_.isEmpty)

  private def getValuesInOrder(mergedMatches: Map[Long, Option[MatchDetail]]): Seq[MatchDetail] =
    mergedMatches.values.filter(_.isDefined).map(_.get).toSeq.sortBy(_.matchId)

  protected def createMatchServiceActor: ActorRef =
    context.actorOf(MatchService.props)
}
