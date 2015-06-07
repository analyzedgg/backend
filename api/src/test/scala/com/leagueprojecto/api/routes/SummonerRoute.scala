package com.leagueprojecto.api.routes

import akka.actor.ActorRef
import akka.actor.Status.Failure
import akka.http.marshallers.sprayjson.SprayJsonSupport._
import akka.http.model.ContentTypes._
import akka.http.model.StatusCodes._
import akka.testkit.TestActor
import com.leagueprojecto.api.domain.Summoner
import com.leagueprojecto.api.services.CacheService.CachedResponse
import com.leagueprojecto.api.services.riot.SummonerService.{GetSummonerByName, SummonerNotFound}

class SummonerRoute extends RoutesTest {
  val endpoint = "/api/euw/summoner"
  val validSummoner = Summoner(123, "Wagglez", 1, 1372782894000L, 30)

  summonerProbe.setAutoPilot(new TestActor.AutoPilot {
    def run(sender: ActorRef, msg: Any): TestActor.AutoPilot = {
      msg match {
        case GetSummonerByName(_, validSummoner.name) =>
          sender ! CachedResponse[Summoner](validSummoner, 1010101010)
        case GetSummonerByName(_, "NotExistingSummoner") =>
          sender ! Failure(new SummonerNotFound(""))
      }
      TestActor.KeepRunning
    }
  })

  "Summoner path" should "return a json response with a CachedResponse in it" in {
    Get(s"$endpoint/${validSummoner.name}") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`

      val response = responseAs[CachedResponse[Summoner]]
      response.response shouldBe validSummoner
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
