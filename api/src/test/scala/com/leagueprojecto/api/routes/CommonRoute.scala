package com.leagueprojecto.api.routes

import akka.actor.ActorRef
import akka.actor.Status.Failure
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.RawHeader
import akka.testkit.TestActor
import com.leagueprojecto.api.domain.Summoner
import com.leagueprojecto.api.services.CacheService.CachedResponse
import com.leagueprojecto.api.services.SummonerManager.GetSummoner
import com.leagueprojecto.api.services.riot.RiotService.{ServiceNotAvailable, TooManyRequests}

class CommonRoute extends RoutesTest {

  val validSummoner = Summoner(123, "Wagglez", 1, 1372782894000L, 30)

  summonerProbe.setAutoPilot(new TestActor.AutoPilot {
    def run(sender: ActorRef, msg: Any): TestActor.AutoPilot = {
      msg match {
        case GetSummoner(_, "Wagglez") => sender ! CachedResponse[Summoner](validSummoner, 1010101010)
        case GetSummoner(_, "_TooManyRequests") => sender ! Failure(new TooManyRequests(""))
        case GetSummoner(_, "_ServiceUnavailable") => sender ! Failure(new ServiceNotAvailable(""))
        case GetSummoner(_, "_InternalServerError") => sender ! Failure(new ArrayIndexOutOfBoundsException())
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
      val corsHeaders = List(RawHeader("Access-Control-Allow-Origin", "*"),
        RawHeader("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS, DELETE"),
        RawHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Authorization"))

      headers should contain theSameElementsAs corsHeaders
    }
  }

}

