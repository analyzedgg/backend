package com.leagueprojecto.api.services

import akka.actor.{ActorLogging, ActorRef, Props, Actor}
import com.leagueprojecto.api.domain.{MatchHistory, Summoner}
import com.leagueprojecto.api.services.riot.{MatchHistoryService, SummonerService}
import com.statsovertime.api.domain.MatchHistory
import MatchHistoryService.GetMatchHistory
import SummonerService.GetSummonerByName

object MatchesService {
  case class GetMatches(region: String, summonerName: String)
  case class Matches(result: List[Summoner])

  def props: Props = Props[MatchesService]
}

class MatchesService extends Actor with ActorLogging {
  import MatchesService._

  var origSender: ActorRef = ActorRef.noSender
  var region: String = null

  val summonerService = context.actorOf(SummonerService.props)
  val matchHistoryService = context.actorOf(SummonerService.props)


  override def receive: Receive = {
    case GetMatches(regionParam, summonerName) =>
      this.region = regionParam
      origSender = sender()
      summonerService ! GetSummonerByName(region, summonerName)

    case summoner: Summoner =>
      matchHistoryService ! GetMatchHistory(region, summoner.id, 15)

    case matchHistoryList: List[MatchHistory] =>
      origSender ! Matches(matchHistoryList)
  }
}
