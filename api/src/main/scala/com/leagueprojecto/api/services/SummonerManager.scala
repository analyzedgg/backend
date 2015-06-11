package com.leagueprojecto.api.services

import akka.actor.{Props, Actor}
import akka.util.Timeout
import com.leagueprojecto.api.domain.Summoner
import com.leagueprojecto.api.services.MatchCombiner.AllMatches
import com.leagueprojecto.api.services.MatchCombiner.GetAllMatches
import com.leagueprojecto.api.services.MatchCombiner.{AllMatches, GetAllMatches}
import com.leagueprojecto.api.services.MatchHistoryManager.GetMatches
import akka.pattern.ask
import com.leagueprojecto.api.services.MatchHistoryManager.GetMatches
import com.leagueprojecto.api.services.SummonerManager.GetSummoner
import com.leagueprojecto.api.services.riot.SummonerService
import com.leagueprojecto.api.services.riot.SummonerService.GetSummonerByName
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object SummonerManager {
  case class GetSummoner(region: String, name: String)

  def props = Props[SummonerManager]
}
class SummonerManager extends Actor {
  implicit val timeout: Timeout = 10.second

  override def receive: Receive = {
    case GetSummoner(region, name) =>
      val summoner = context.actorOf(SummonerService.props(region, name))
      val originalSender = sender()

      (summoner ? GetSummonerByName) onSuccess {
        case summoner: Summoner =>
          println(originalSender)
          originalSender ! summoner
        case response: Any =>
          println(s"Got different response: $response")
      }
  }
}

