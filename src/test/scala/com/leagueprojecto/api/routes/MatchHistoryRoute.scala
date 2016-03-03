//package com.leagueprojecto.api.routes
//
//import akka.actor.ActorRef
//import akka.actor.Status.Failure
//import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
//import akka.http.scaladsl.model.ContentTypes._
//import akka.http.scaladsl.model.StatusCodes._
//import akka.testkit.TestActor
//import com.leagueprojecto.api.domain.{PlayerStats, MatchDetail}
//import com.leagueprojecto.api.services.CacheService.CachedResponse
//import com.leagueprojecto.api.services.MatchHistoryManager.GetMatches
//import com.leagueprojecto.api.services.riot.SummonerService.SummonerNotFound
//
//class MatchHistoryRoute extends RoutesTest {
//  val endpoint = "/api/euw/matchhistory"
//
//  val validSummonerId = 123456789
//  val invalidSummonerId = 987654321
//
//  val validPlayerStats = PlayerStats(10, 5, 3, 2)
//  val validHistory =
//    MatchDetail("RANKED_SOLO_5x5", 1600, 1432328493438L, validPlayerStats, 100, "DUO_SUPPORT", "BOTTOM", winner = true)
//  val validHistoryList = List(validHistory)
//
//  matchHistoryProbe.setAutoPilot(new TestActor.AutoPilot {
//    def run(sender: ActorRef, msg: Any): TestActor.AutoPilot = {
//      msg match {
//        case GetMatches(_, 123456789, _, _) =>
//          sender ! CachedResponse[List[MatchDetail]](validHistoryList, 1010101010)
//        case GetMatches(_, 987654321, _, _) =>
//          sender ! Failure(new SummonerNotFound(""))
//      }
//      TestActor.KeepRunning
//    }
//  })
//
//  "MatchHistory path" should "return a json response with a CachedResponse in it" in {
//    Get(s"$endpoint/$validSummonerId") ~> routes ~> check {
//      status shouldBe OK
//      contentType shouldBe `application/json`
//
//      val response = responseAs[CachedResponse[List[MatchDetail]]]
//      response.response shouldBe validHistoryList
//    }
//  }
//
//  it should "return an empty response when the summoner id does not exist" in {
//    Get(s"$endpoint/$invalidSummonerId") ~> routes ~> check {
//      status shouldBe NotFound
//
//      responseAs[String] shouldBe ""
//    }
//  }
//
//  it should "always send Options back on requests" in {
//    Options(s"$endpoint/$validSummonerId") ~> routes ~> check {
//      status shouldBe OK
//      responseAs[String] shouldBe ""
//    }
//  }
//}
