package com.leagueprojecto.api.routes

import akka.actor.ActorRef
import akka.event.LoggingAdapter
import akka.http.testkit.ScalatestRouteTest
import akka.testkit.TestProbe
import com.leagueprojecto.api.Routes
import com.typesafe.config.Config
import org.scalatest.{FlatSpec, GivenWhenThen, Matchers}

abstract class RoutesTest extends FlatSpec with Matchers with GivenWhenThen with ScalatestRouteTest with Routes {
  override def config: Config = testConfig
  override val logger: LoggingAdapter = system.log

  val summonerProbe = new TestProbe(system)
  val matchHistoryProbe = new TestProbe(system)

  override val cachedSummonerService: ActorRef = summonerProbe.ref
  override val cachedMatchHistoryService: ActorRef = matchHistoryProbe.ref
}
