package com.leagueprojecto.api.routes

import akka.actor.ActorRef
import akka.actor.Status.Failure
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.StatusCodes._
import akka.testkit.{TestProbe, TestActor}
import com.leagueprojecto.api.domain.Summoner
import com.leagueprojecto.api.services.SummonerManager
import com.leagueprojecto.api.services.SummonerManager.GetSummoner
import com.leagueprojecto.api.services.riot.SummonerService.SummonerNotFound

class SummonerRoute extends RoutesTest {
  val endpoint = "/api/euw/summoner"
  val validSummoner = Summoner(123, "Wagglez", 1, 1372782894000L, 30)

  override def setSummonerAutoPilot(probe: TestProbe) = {
    probe.setAutoPilot(new TestActor.AutoPilot {
      def run(sender: ActorRef, msg: Any): TestActor.AutoPilot = {
        msg match {
          case GetSummoner(_, validSummoner.name) =>
            sender ! SummonerManager.Result(validSummoner)
          case GetSummoner(_, "NotExistingSummoner") =>
            sender ! Failure(SummonerNotFound)
        }
        TestActor.KeepRunning
      }
    })
  }

  "Summoner path" should "return a json response with a Summoner in it" in {
    Get(s"$endpoint/${validSummoner.name}") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`

      responseAs[Summoner] shouldBe validSummoner
    }
  }

  it should "return a 404 when the Summoner does not exist" in {
    Get(s"$endpoint/NotExistingSummoner") ~> routes ~> check {
      status shouldBe NotFound
      responseAs[String] shouldBe ""
    }
  }

  it should "always send Options back on requests" in {
    Options("/api/euw/summoner/Wagglez") ~> routes ~> check {
      status shouldBe OK
      responseAs[String] shouldBe ""
    }
  }
}
