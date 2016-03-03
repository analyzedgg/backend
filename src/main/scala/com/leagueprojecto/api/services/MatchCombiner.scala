package com.leagueprojecto.api.services

import akka.actor.{ActorLogging, Props, Actor}
import com.leagueprojecto.api.domain.MatchDetail
import com.leagueprojecto.api.services.MatchCombiner.{TimeOut, GetMatches}
import com.leagueprojecto.api.services.riot.MatchService
import com.leagueprojecto.api.services.riot.MatchService.GetMatch
import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object MatchCombiner {
  case object GetMatches
  case object TimeOut

  def props(regionParam: String, summonerId: Long, matchIds: Seq[Long]) = Props(new MatchCombiner(regionParam, summonerId, matchIds))
}

class MatchCombiner(regionParam: String, summonerId: Long, matchIds: Seq[Long]) extends Actor with ActorLogging {
  var matches = mutable.ArrayBuffer.empty[MatchDetail]
  var originalSender = Actor.noSender

  override def receive = {
    case GetMatches =>
      originalSender = sender()
      matchIds.foreach(id => context.actorOf(MatchService.props(regionParam, summonerId, id)) ! GetMatch)
      context.system.scheduler.scheduleOnce(5 seconds, self, TimeOut)

    case matchDetail: MatchDetail =>
      log.debug(s"Got a match in combiner: $matchDetail")
      matches += matchDetail

      if (matchIds.size == matches.size) {
        log.info(s"MatchCombiner received all matches, returning ${matches.size} matches.")
        originalSender ! matches
      }

    case TimeOut =>
      log.info(s"MatchCombiner timed out, returning ${matches.size} matches.")
      originalSender ! matches
  }
}
