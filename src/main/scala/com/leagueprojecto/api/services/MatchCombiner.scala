package com.leagueprojecto.api.services

import akka.actor._
import com.leagueprojecto.api.domain.MatchDetail
import com.leagueprojecto.api.services.MatchCombiner._
import com.leagueprojecto.api.services.riot.MatchService
import com.leagueprojecto.api.services.riot.MatchService.GetMatch
import scala.concurrent.duration._

object MatchCombiner {
  case object GetMatches
  case object TimeOut

  sealed trait State
  case object Idle extends State
  case object GettingMatches extends State

  case class StateData(sender: ActorRef, matches: Seq[MatchDetail])
  case class Result(matches: Seq[MatchDetail])

  def props(regionParam: String, summonerId: Long, matchIds: Seq[Long]) = Props(new MatchCombiner(regionParam, summonerId, matchIds))
}

class MatchCombiner(regionParam: String, summonerId: Long, matchIds: Seq[Long]) extends FSM[State, StateData] with ActorLogging {
  startWith(Idle, StateData(Actor.noSender, Seq.empty))

  when(Idle) {
    case Event(GetMatches, _) =>
      matchIds.foreach(id => context.actorOf(MatchService.props(regionParam, summonerId, id)) ! GetMatch)
      goto(GettingMatches) using StateData(sender(), Seq.empty)
  }

  when(GettingMatches, stateTimeout = 5 seconds) {
    case Event(matchDetail: MatchDetail, state @ StateData(sender, matches)) if matches.size + 1 == matchIds.size =>
      sender ! Result(matches :+ matchDetail)
      stop()

    case Event(matchDetail: MatchDetail, state @ StateData(sender, matches)) =>
      goto(GettingMatches) using state.copy(matches = matches :+ matchDetail)

    case Event(StateTimeout, StateData(sender, matches)) =>
      log.info(s"MatchCombiner timed out, returning ${matches.size} matches.")
      sender ! Result(matches)
      stop()
  }
}
