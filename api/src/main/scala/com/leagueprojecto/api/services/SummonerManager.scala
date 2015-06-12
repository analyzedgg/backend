package com.leagueprojecto.api.services

import akka.actor.{Props, Actor}
import akka.util.Timeout
import akka.pattern.ask
import com.leagueprojecto.api.services.SummonerManager.GetSummoner
import com.leagueprojecto.api.services.riot.SummonerService
import com.leagueprojecto.api.services.riot.SummonerService.GetSummonerByName
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.pattern.pipe

object SummonerManager {
  case class GetSummoner(region: String, name: String)

  def props = Props[SummonerManager]
}
class SummonerManager extends Actor {
  implicit val timeout: Timeout = 10.second

  override def receive: Receive = {
    case GetSummoner(region, name) =>
      val summoner = context.actorOf(SummonerService.props(region, name))

      (summoner ? GetSummonerByName) pipeTo sender() onComplete {
        case _ => context.stop(summoner)
      }
  }
}

