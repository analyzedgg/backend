package com.leagueprojecto.api.services

import akka.actor._
import com.leagueprojecto.api.domain.MatchDetail
import com.leagueprojecto.api.services.MatchCombiner._
import com.leagueprojecto.api.services.riot.MatchService
import com.leagueprojecto.api.services.riot.MatchService.GetMatch
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
    case Event(matchDetail: MatchDetail, state @ StateData(sender, matches)) =>
      val newMatches = matches + (matchDetail.matchId -> Some(matchDetail))

      if (hasEmptyValues(newMatches)) {
        goto(GettingMatches) using state.copy(matches = newMatches)
      } else {
        sender ! Result(getValues(newMatches))
        stop()
      }

    case Event(StateTimeout, StateData(sender, matches)) =>
      log.info(s"MatchCombiner timed out, returning ${matches.size} matches.")
      sender ! Result(getValues(matches))
      stop()
  }

  private def hasEmptyValues(mergedMatches: Map[Long, Option[MatchDetail]]): Boolean =
    mergedMatches.exists(_._2.isEmpty)

  private def getValues(mergedMatches: Map[Long, Option[MatchDetail]]): Seq[MatchDetail] =
    mergedMatches.values.filter(_.isDefined).map(_.get).toSeq

  protected def createMatchServiceActor: ActorRef =
    context.actorOf(MatchService.props)
}
