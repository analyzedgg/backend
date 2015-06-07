package com.leagueprojecto.api.routes

import akka.actor.ActorRef
import akka.actor.Status.Failure
import akka.http.model.StatusCodes._
import akka.testkit.TestActor
import com.leagueprojecto.api.domain.Summoner
import com.leagueprojecto.api.services.CacheService.CachedResponse
import com.leagueprojecto.api.services.riot.RiotService.{ServiceNotAvailable, TooManyRequests}
import com.leagueprojecto.api.services.riot.SummonerService.GetSummonerByName

class CommonRoute extends RoutesTest {

  val validSummoner = Summoner(123, "Wagglez", 1, 1372782894000L, 30)

  summonerProbe.setAutoPilot(new TestActor.AutoPilot {
    def run(sender: ActorRef, msg: Any): TestActor.AutoPilot = {
      msg match {
        case GetSummonerByName(_, "Wagglez") => sender ! CachedResponse[Summoner](validSummoner, 1010101010)
        case GetSummonerByName(_, "_TooManyRequests") => sender ! Failure(new TooManyRequests(""))
        case GetSummonerByName(_, "_ServiceUnavailable") => sender ! Failure(new ServiceNotAvailable(""))
        case GetSummonerByName(_, "_InternalServerError") => sender ! Failure(new ArrayIndexOutOfBoundsException())
      }
      TestActor.KeepRunning
    }
  })

  "Routes" should "work with all valid region notations" in {
    val validRegions = List("BR", "EUNE", "EUW", "KR", "LAN", "LAS", "NA", "OCE", "RU", "TR")
    val validRegionsLowercase = validRegions.map(_.toLowerCase)

    (validRegions ::: validRegionsLowercase).foreach { region =>
      Get(s"/api/$region/summoner/Wagglez") ~> routes ~> check {
        status shouldBe OK
      }
    }
  }

  it should "return a ServiceUnavailable when the riot API returns a ServiceUnavailable" in {
    Get(s"/api/euw/summoner/_TooManyRequests") ~> routes ~> check {
      status shouldBe TooManyRequests
    }
  }

  it should "return a TooManyRequests when the riot API returns a TooManyRequests" in {
    Get(s"/api/euw/summoner/_ServiceUnavailable") ~> routes ~> check {
      status shouldBe ServiceUnavailable
    }
  }

  it should "InternalServerError when any other exception in thrown" in {
    Get(s"/api/euw/summoner/_InternalServerError") ~> routes ~> check {
      status shouldBe InternalServerError
    }
  }

  it should "always have CORS headers on the response back" in {
    Get("/api/euw/summoner/Wagglez") ~> routes ~> check {
      headers should contain theSameElementsAs corsHeaders
    }
  }

}

