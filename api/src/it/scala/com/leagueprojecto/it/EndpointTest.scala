package com.leagueprojecto.it

import akka.actor.ActorRef
import akka.event.LoggingAdapter
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.leagueprojecto.api.Routes
import com.leagueprojecto.api.domain.{MatchHistory, Summoner}
import com.leagueprojecto.api.services.{CacheService, MatchHistoryManager, SummonerManager}
import com.typesafe.config.Config
import org.scalatest.{FlatSpec, GivenWhenThen, Matchers}

abstract class EndpointTest extends FlatSpec with Matchers with GivenWhenThen with ScalatestRouteTest with Routes {
  override def config: Config = testConfig
  override val logger: LoggingAdapter = system.log

  // Services
  val matchHistoryService: ActorRef = system.actorOf(MatchHistoryManager.props)

  // Service caches
  val matchhistoryCacheTime = 5000
  override val cachedMatchHistoryService: ActorRef =
    system.actorOf(CacheService.props[List[MatchHistory]](matchHistoryService, matchhistoryCacheTime))
}
