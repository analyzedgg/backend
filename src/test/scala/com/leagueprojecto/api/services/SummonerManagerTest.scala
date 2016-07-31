package com.leagueprojecto.api.services

import akka.actor.Status.Failure
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.CircuitBreaker
import akka.testkit.{TestActor, TestProbe}
import com.leagueprojecto.api.domain.Summoner
import com.leagueprojecto.api.services.couchdb.DatabaseService
import com.leagueprojecto.api.services.riot.SummonerService
import org.scalatest.{FlatSpec, GivenWhenThen, Matchers}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class SummonerManagerTest extends FlatSpec with Matchers with GivenWhenThen {
  val system = ActorSystem.create()
  val executor: ExecutionContextExecutor = system.dispatcher
  val dbProbe = new TestProbe(system)
  val riotProbe = new TestProbe(system)

  lazy val couchDbCircuitBreaker =
    new CircuitBreaker(system.scheduler, maxFailures = 5, callTimeout = 10.seconds, resetTimeout = 1.minute)(executor)
  val testRegion = "EUW"
  val testName = "Wagglez"
  val testNameNoDb = "No db"
  val testNameNoDbNorRiot = "No db nor riot"
  val testSummoner = Summoner(123123123, testName, 100, 1434315156000L, 30)

  class TestSummonerManager extends SummonerManager(couchDbCircuitBreaker) {
    override protected def createDatabaseServiceActor: ActorRef = dbProbe.ref
    override protected def createSummonerServiceActor: ActorRef = riotProbe.ref
  }

  trait Fixture {
    val actorRef = system.actorOf(Props(new TestSummonerManager))
  }

  dbProbe.setAutoPilot(new TestActor.AutoPilot {
    def run(sender: ActorRef, msg: Any): TestActor.AutoPilot = {
      msg match {
        case DatabaseService.GetSummoner(_, "No db") => sender ! DatabaseService.NoResult
        case DatabaseService.GetSummoner(_, "No db nor riot") => sender ! DatabaseService.NoResult
        case DatabaseService.GetSummoner(_, _) => sender ! DatabaseService.SummonerResult(testSummoner)
        case DatabaseService.SaveSummoner(_, _) => sender ! DatabaseService.SummonerSaved
      }
      TestActor.KeepRunning
    }
  })

  riotProbe.setAutoPilot(new TestActor.AutoPilot {
    def run(sender: ActorRef, msg: Any): TestActor.AutoPilot = {
      msg match {
        case SummonerService.GetSummonerByName(_, "No db nor riot") => sender ! SummonerService.SummonerNotFound
        case SummonerService.GetSummonerByName(_, _) => sender ! SummonerService.Result(testSummoner)
      }
      TestActor.KeepRunning
    }
  })

  "SummonerManager" should "request Summoner info from the database and return the result without consulting Riot" in new Fixture {
    Given("a mocked sender probe")
    val senderProbe = new TestProbe(system)

    When("a request to the SummonerManager is done")
    senderProbe.send(actorRef, SummonerManager.GetSummoner(testRegion, testName))

    Then("the Summoner info should be requested from the DatabaseService")
    dbProbe.expectMsg(DatabaseService.GetSummoner(testRegion, testName))

    And("the SummonerManager should not contact the riotProbe")
    riotProbe.expectNoMsg(100.milliseconds)

    And("the SummonerManager should return the result to the senderProbe")
    senderProbe.expectMsg(SummonerManager.Result(testSummoner))
  }

  it should "request Summoner info from Riot when the database has none" in new Fixture {
    Given("a mocked sender probe")
    val senderProbe = new TestProbe(system)

    When("a request to the SummonerManager is done")
    senderProbe.send(actorRef, SummonerManager.GetSummoner(testRegion, testNameNoDb))

    Then("the Summoner info should be requested from the DatabaseService")
    dbProbe.expectMsg(DatabaseService.GetSummoner(testRegion, testNameNoDb))

    And("the Summoner info should be requested from the SummonerService")
    riotProbe.expectMsg(SummonerService.GetSummonerByName(testRegion, testNameNoDb))

    And("the SummonerManager should return the result to the senderProbe")
    senderProbe.expectMsg(SummonerManager.Result(testSummoner))

    And("the Summoner should get saved in the database")
    dbProbe.expectMsg(DatabaseService.SaveSummoner(testRegion, testSummoner))
  }

  it should "return a Summoner not found when the Summoner does not exist at all" in new Fixture {
    Given("a mocked sender probe")
    val senderProbe = new TestProbe(system)

    When("a request to the SummonerManager is done")
    senderProbe.send(actorRef, SummonerManager.GetSummoner(testRegion, testNameNoDbNorRiot))

    Then("the Summoner info should be requested from the DatabaseService")
    dbProbe.expectMsg(DatabaseService.GetSummoner(testRegion, testNameNoDbNorRiot))

    And("the Summoner info should be requested from the SummonerService")
    riotProbe.expectMsg(SummonerService.GetSummonerByName(testRegion, testNameNoDbNorRiot))

    And("the SummonerManager should return the failure")
    senderProbe.expectMsgClass(classOf[Failure])

    And("the not existing Summoner should not get saved in the database")
    dbProbe.expectNoMsg(100.milliseconds)
  }
}
