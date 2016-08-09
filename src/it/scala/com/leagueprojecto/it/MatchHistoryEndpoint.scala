//package com.analyzedgg.it
//
//import akka.http.scaladsl.model.ContentTypes._
//import akka.http.scaladsl.model.StatusCodes._
//import akka.http.scaladsl.testkit.RouteTestTimeout
//import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
//import com.analyzedgg.api.domain.{MatchDetail, PlayerStats}
//import scala.concurrent.duration._
//
//class MatchHistoryEndpoint extends EndpointTest {
//  val endpoint = "/api/euw/matchhistory"
//
//  implicit val routeTestTimeout = RouteTestTimeout(5.second)
//
//  val validSummonerId = 52477463
//
//  val validPlayerStats = PlayerStats(10, 5, 3, 2)
//  val validHistory =
//    MatchDetail("RANKED_SOLO_5x5", 1600, 1432328493438L, validPlayerStats, 100, "DUO_SUPPORT", "BOTTOM", winner = true)
//  val validHistoryList = List(validHistory)
//
//  "MatchHistory endpoint" should "return a json response with a CachedResponse in it" in {
//    Get(s"$endpoint/$validSummonerId") ~> routes ~> check {
//      status shouldBe OK
//      contentType shouldBe `application/json`
//
//      val response = responseAs[CachedResponse[List[MatchDetail]]]
//      response.response.length shouldBe 60
//    }
//  }
//}
