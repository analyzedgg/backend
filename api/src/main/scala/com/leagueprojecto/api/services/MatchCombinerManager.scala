package com.leagueprojecto.api.services

import akka.actor.{Props, Actor}
import akka.util.Timeout
import com.leagueprojecto.api.services.MatchCombiner.{AllMatches, GetAllMatches}
import com.leagueprojecto.api.services.MatchCombinerManager.GetMatches
import akka.pattern.ask
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object MatchCombinerManager {
  case class GetMatches(region: String, summonerId: Long)

  def props = Props[MatchCombinerManager]
}
class MatchCombinerManager extends Actor {
  implicit val timeout: Timeout = 10.second

  override def receive: Receive = {
    case GetMatches(region, summonerId) =>
      val matchCombiner = context.actorOf(MatchCombiner.props(region, summonerId))
      val originalSender = sender()

      (matchCombiner ? GetAllMatches) onSuccess {
        case AllMatches(list) =>
          println(originalSender)
          originalSender ! list
        case response: Any =>
          println(s"Got different response: $response")
      }
  }
}
