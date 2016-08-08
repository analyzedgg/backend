package com.leagueprojecto.api.testHelpers

import akka.event.LoggingAdapter
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestProbe
import com.leagueprojecto.api.Routes
import com.typesafe.config.Config
import org.scalatest.{FlatSpec, GivenWhenThen, Matchers}

abstract class RoutesHelper extends FlatSpec with Matchers with GivenWhenThen with ScalatestRouteTest with Routes {
  override def config: Config = testConfig
  override val logger: LoggingAdapter = system.log

  override def createSummonerActor = {
    val summonerProbe = new TestProbe(system)
    setSummonerAutoPilot(summonerProbe)
    summonerProbe.ref
  }

  override def createMatchHistoryActor = {
    val matchHistoryProbe = new TestProbe(system)
    setMatchHistoryAutoPilot(matchHistoryProbe)
    matchHistoryProbe.ref
  }

  def setSummonerAutoPilot(probe: TestProbe) = ()
  def setMatchHistoryAutoPilot(probe: TestProbe) = ()
}
