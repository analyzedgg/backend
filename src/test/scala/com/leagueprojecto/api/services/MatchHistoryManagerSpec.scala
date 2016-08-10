package com.leagueprojecto.api.services

import akka.actor.ActorSystem
import akka.pattern.CircuitBreaker
import akka.testkit.TestProbe
import org.scalatest.{FlatSpec, GivenWhenThen, Matchers}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class MatchHistoryManagerSpec extends FlatSpec with Matchers with GivenWhenThen {
  val system = ActorSystem.create()
  val executor: ExecutionContextExecutor = system.dispatcher
  val dbProbe = new TestProbe(system)
  val riotProbe = new TestProbe(system)

  lazy val couchDbCircuitBreaker =
    new CircuitBreaker(system.scheduler, maxFailures = 5, callTimeout = 10.seconds, resetTimeout = 1.minute)(executor)


}
