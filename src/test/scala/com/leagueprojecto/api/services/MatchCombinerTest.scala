package com.leagueprojecto.api.services

import akka.actor.FSM.StateTimeout
import akka.actor.{Props, ActorRef, ActorSystem}
import akka.testkit.{TestActor, TestProbe}
import com.leagueprojecto.api.domain.{PlayerStats, MatchDetail}
import com.leagueprojecto.api.services.riot.MatchService
import org.scalatest.{GivenWhenThen, Matchers, FlatSpec}

class MatchCombinerTest extends FlatSpec with Matchers with GivenWhenThen {
  val system = ActorSystem.create()
  val riotProbe = new TestProbe(system)

  val testMatchIds: Seq[Long] = List(1, 2, 3, 4, 5)
  val testSummonerId = 123123123
  val testRegion = "EUW"
  val testMatchDetail = MatchDetail(0, "SOLOQ", 1400, 123123123L, 234234234L, 100, "DUO_CARRY", "BOT", winner = true, PlayerStats(100, 1, 2, 3))

  class TestMatchCombiner extends MatchCombiner {
    override protected def createMatchServiceActor: ActorRef = riotProbe.ref
  }

  trait Fixture {
    val actorRef = system.actorOf(Props(new TestMatchCombiner))
  }

  riotProbe.setAutoPilot(new TestActor.AutoPilot {
    def run(sender: ActorRef, msg: Any): TestActor.AutoPilot = {
      msg match {
        case MatchService.GetMatch(_, _, 10) =>

        case MatchService.GetMatch(_, _, matchId) =>
          sender ! MatchService.Result(testMatchDetail.copy(matchId = matchId))
      }
      TestActor.KeepRunning
    }
  })

  "MatchCombiner" should "request MatchDetails from Riot" in new Fixture {
    Given("a list of expected messages which should be sent to Riot")
    val expectedMessages = testMatchIds.map(MatchService.GetMatch(testRegion, testSummonerId, _))

    When("a request to the MatchCombiner is done")
    actorRef ! MatchCombiner.GetMatches(testRegion, testSummonerId, testMatchIds)

    Then("the expected messages should received at the riot probe")
    riotProbe.expectMsgAllOf(expectedMessages: _*)
  }

  it should "return a list of MatchDetails back to the sender" in new Fixture {
    Given("a mocked caller probe")
    val senderProbe = new TestProbe(system)

    And("an expected list of MatchDetails")
    val expectedResult = testMatchIds.map(id => testMatchDetail.copy(matchId = id)).seq

    When("a request to the MatchCombiner is done from the sender probe")
    senderProbe.send(actorRef, MatchCombiner.GetMatches(testRegion, testSummonerId, testMatchIds))

    Then("a list of matches should be returned to the sender probe")
    senderProbe.expectMsg(MatchCombiner.Result(expectedResult))
  }

  it should "return all available MatchDetails even if not everything was received from Riot" in new Fixture {
    Given("a mocked caller probe")
    val senderProbe = new TestProbe(system)

    And("an expected list of MatchDetails")
    val expectedResult = testMatchIds.map(id => testMatchDetail.copy(matchId = id)).seq

    And("the standard list with a false one added")
    val alteredMatchIds = testMatchIds :+ 10L

    When("a request to the MatchCombiner is done from the sender probe")
    senderProbe.send(actorRef, MatchCombiner.GetMatches(testRegion, testSummonerId, alteredMatchIds))

    And("a timeout is created")
    senderProbe.send(actorRef, StateTimeout)

    Then("a list of matches should be returned to the sender probe")
    senderProbe.expectMsg(MatchCombiner.Result(expectedResult))
  }
}
