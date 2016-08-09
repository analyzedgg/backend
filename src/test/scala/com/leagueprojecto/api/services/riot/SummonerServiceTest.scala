//package com.analyzedgg.api.services.riot
//
//import akka.actor.Status.Failure
//import akka.actor.{Props, ActorSystem}
//import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
//import akka.stream.scaladsl.Flow
//import com.analyzedgg.api.domain.Summoner
//import com.analyzedgg.api.services.riot.RiotService.{ServiceNotAvailable, TooManyRequests}
//import com.analyzedgg.api.services.riot.SummonerService.{SummonerNotFound, GetSummonerByName}
//import akka.http.scaladsl.model.StatusCodes._
//
//class SummonerServiceTest(_system: ActorSystem) extends BaseServiceTests(_system) {
//
//  def this() = this(ActorSystem("SummonerServiceTest"))
//
//  "SummonerService" should {
//    "return a Summoner object with a valid user" in {
//      Given("a valid HTTP response")
//      val json = readFileFromClasspath("responses/summoner/happyflow.json")
//      val response = HttpResponse(status = OK, entity = json)
//
//      createActorAndSendMessage(response)
//
//      Then("a Summoner object should be returned")
//      val result = expectMsgClass(classOf[Summoner])
//
//      And("the name should equal 'Wagglez'")
//      result.name shouldBe "Wagglez"
//    }
//
//    "return a Failure(SummonerNotFound) on a 404" in {
//      Given("a 404 HTTP response")
//      val response = HttpResponse(status = NotFound)
//
//      createActorAndSendMessage(response)
//
//      Then("a Failure object should be returned")
//      val result = expectMsgClass(classOf[Failure])
//
//      And("the failure should wrap a SummonerNotFound exception")
//      result.cause shouldBe a[SummonerNotFound]
//    }
//
//    "return a Failure(TooManyRequests) on a 429" in {
//      Given("a 429 HTTP response")
//      val response = HttpResponse(status = TooManyRequests)
//
//      createActorAndSendMessage(response)
//
//      Then("a Failure object should be returned")
//      val result = expectMsgClass(classOf[Failure])
//
//      And("the failure should wrap a TooManyRequests exception")
//      result.cause shouldBe a[TooManyRequests]
//    }
//
//    "return a Failure(ServiceNotAvailable) on a 503" in {
//      Given("a 503 HTTP response")
//      val response = HttpResponse(status = ServiceUnavailable)
//
//      createActorAndSendMessage(response)
//
//      Then("a Failure object should be returned")
//      val result = expectMsgClass(classOf[Failure])
//
//      And("the failure should wrap a ServiceNotAvailable exception")
//      result.cause shouldBe a[ServiceNotAvailable]
//    }
//
//    "return a Failure(InternalServerError) on a 503" in {
//      Given("a random HTTP response")
//      val response = HttpResponse(status = UnavailableForLegalReasons)
//
//      createActorAndSendMessage(response)
//
//      Then("a Failure object should be returned")
//      val result = expectMsgClass(classOf[Failure])
//
//      And("the failure should wrap a IllegalStateException exception")
//      result.cause shouldBe a[IllegalStateException]
//    }
//  }
//
//  def createActorAndSendMessage(response: HttpResponse): Unit = {
//    And("a SummonerService actor")
//    val actorRef = system.actorOf(Props(new MockedSummonerService(response)))
//
//    When("summoner information is requested")
//    actorRef ! GetSummonerByName
//  }
//
//  class MockedSummonerService(httpResponse: HttpResponse) extends SummonerService("REGION", "USERNAME") {
//    override lazy val riotConnectionFlow: Flow[HttpRequest, HttpResponse, Any] = Flow[HttpRequest].map { request =>
//      httpResponse
//    }
//  }
//}
