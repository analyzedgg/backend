package com.leagueprojecto.api.services

import akka.actor.{Props, Actor}
import akka.util.Timeout
import com.leagueprojecto.api.services.MatchCombiner.{AllMatches, GetAllMatches}
import com.leagueprojecto.api.services.MatchHistoryManager.GetMatches
import akka.pattern.ask
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.pattern.pipe

object MatchHistoryManager {
  case class GetMatches(region: String, summonerId: Long, queueType: String, championList: String)

  def props = Props[MatchHistoryManager]
}
class MatchHistoryManager extends Actor {
  implicit val timeout: Timeout = 1.minute

  override def receive: Receive = {
    case GetMatches(region, summonerId, queueType, championList) =>
      val matchCombiner = context.actorOf(MatchCombiner.props(region, summonerId, queueType, championList))

      (matchCombiner ? GetAllMatches) collect {
        case AllMatches(list) => list
      } pipeTo sender() onComplete {
        case _ => context.stop(matchCombiner)
      }
  }
}
