package com.leagueprojecto.api.services

import akka.actor.{Props, Actor}
import com.leagueprojecto.api.domain.MatchHistory
import com.leagueprojecto.api.services.MatchCombiner.{AllMatches, GetAllMatches}
import com.leagueprojecto.api.services.riot.MatchHistoryService
import com.leagueprojecto.api.services.riot.MatchHistoryService.{MatchHistoryList, GetMatchHistory}

object MatchCombiner {
  case object GetAllMatches
  case class AllMatches(list: List[MatchHistory])

  def props(region: String, summonerId: Long) = Props(new MatchCombiner(region, summonerId))
}
class MatchCombiner(region: String, summonerId: Long) extends Actor {
  val matchHistoryService = context.actorOf(MatchHistoryService.props(region, summonerId))

  var matches: List[MatchHistory] = List.empty
  var originalSender = Actor.noSender

  override def receive: Receive = {

    case GetAllMatches =>
      originalSender = sender()
      matchHistoryService ! GetMatchHistory(0, 15)

    case MatchHistoryList(matchesResponse) if matchesResponse.length == 15 && (matches.length + 15) < 60 =>
      matches = matches ::: matchesResponse
      matchHistoryService ! GetMatchHistory(matches.length, matches.length + 15)

    case MatchHistoryList(matchesResponse) =>
      matches = matches ::: matchesResponse
      originalSender ! AllMatches(matches)
  }
}
