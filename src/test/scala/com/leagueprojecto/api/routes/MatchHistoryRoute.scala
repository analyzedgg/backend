package com.leagueprojecto.api.routes

import akka.actor.ActorRef
import akka.actor.Status.Failure
import com.leagueprojecto.api.domain.riot.Player
// Do not remove the following import. IntelliJ might say it's not used, but it is for converting json to case classes.
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.StatusCodes._
import akka.testkit.{TestActor, TestProbe}
import com.leagueprojecto.api.domain._
import com.leagueprojecto.api.services.MatchHistoryManager
import com.leagueprojecto.api.services.MatchHistoryManager.GetMatches
import com.leagueprojecto.api.services.riot.SummonerService.SummonerNotFound

class MatchHistoryRoute extends RoutesTest {
  val endpoint = "/api/euw/matchhistory"

  val validSummonerId = 123456789
  val invalidSummonerId = 987654321

  val validPlayerStats = PlayerStats(10, 5, 3, 2)
  val validTeamRed = Team(List(Player(validSummonerId, "Minikoen")))
  val validTeamBlue = Team(List(Player(validSummonerId, "Waggles")))
  val validTeams = Teams(validTeamRed, validTeamBlue)
  val validMatchVersion = "6.3.0.240"
  val validHistory =
    MatchDetail(12312312L, "RANKED_SOLO_5x5", 1600, 1432328493438L, validSummonerId, 100, "DUO_SUPPORT", "BOTTOM", winner = true,
      validMatchVersion, validPlayerStats, validTeams)
  val validHistoryList = List(validHistory)

  override def setMatchHistoryAutoPilot(probe: TestProbe) = {
    probe.setAutoPilot(new TestActor.AutoPilot {
      def run(sender: ActorRef, msg: Any): TestActor.AutoPilot = {
        msg match {
          case GetMatches(_, 123456789, _, _) =>
            sender ! MatchHistoryManager.Result(validHistoryList)
          case GetMatches(_, 987654321, _, _) =>
            sender ! Failure(new SummonerNotFound(""))
        }
        TestActor.KeepRunning
      }
    })
  }

  "MatchHistory path" should "return a json response with a Match History in it" in {
    Get(s"$endpoint/$validSummonerId") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`

      responseAs[Seq[MatchDetail]] shouldBe validHistoryList
    }
  }

  it should "return an empty response when the summoner id does not exist" in {
    Get(s"$endpoint/$invalidSummonerId") ~> routes ~> check {
      status shouldBe NotFound

      responseAs[String] shouldBe ""
    }
  }

  it should "always send Options back on requests" in {
    Options(s"$endpoint/$validSummonerId") ~> routes ~> check {
      status shouldBe OK
      responseAs[String] shouldBe ""
    }
  }
}
