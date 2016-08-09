//package com.analyzedgg.it
//
//import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
//import akka.http.scaladsl.model.ContentTypes._
//import akka.http.scaladsl.model.StatusCodes._
//import com.analyzedgg.api.domain.Summoner
//
//class SummonerEndpoint extends EndpointTest {
//  val endpoint = "/api/euw/summoner"
//  val nonExistingSummoner = "NonExistingSummonerName"
//  val validSummoner = Summoner(52477463, "Wagglez", 781, 1441138071000L, 30)
//
//  "Summoner endpoint" should "return a json response with a CachedResponse in it" in {
//    Get(s"$endpoint/${validSummoner.name}") ~> routes ~> check {
//      status shouldBe OK
//      contentType shouldBe `application/json`
//
//      val response = responseAs[CachedResponse[Summoner]]
//      response.response shouldBe validSummoner
//    }
//  }
//
//  it should "return a 404 when the summoner does not exist" in {
//    Get(s"$endpoint/$nonExistingSummoner") ~> routes ~> check {
//      status shouldBe NotFound
//      responseAs[String] shouldBe ""
//    }
//  }
//}
