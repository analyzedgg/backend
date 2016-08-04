package com.leagueprojecto.api.services.riot

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.HttpResponse
import akka.testkit.TestProbe
import akka.http.scaladsl.model.StatusCodes._
import com.leagueprojecto.api.services.riot.RecentMatchesService.{GetRecentMatchIds, Result}
import com.leagueprojecto.api.testHelpers.BaseServiceHelper

import scala.concurrent.Future

class RecentMatchesSpec(_system: ActorSystem) extends BaseServiceHelper(_system) {
  val probe: TestProbe = TestProbe()

  def this() = this(ActorSystem("RecentMatchesSpec"))

  "RecentMatchesService" should {
    "return a Result containing a List of Match ids" in {
      Given("a valid response")
      val json = readFileFromClasspath("/responses/matchHistory/happyFlow.json")
      val response = HttpResponse(status = OK, entity = json)

      createActorAndSendMessage(response)

      Then("a Result object should be returned")
      val result: Result = probe.expectMsgClass(classOf[Result])

      And("the size should equal the json attributes")
      result.matchIds.size shouldBe 20
    }
    "return a MatchRetrievalFailed containing the failed match id" in {
      Given("A bad response")
      val response = HttpResponse(status = BadRequest)

      val failingActor = createActorAndSendMessage(response)
      val stopWatchProbe = TestProbe()
      stopWatchProbe watch failingActor

      And("the actor should be stopped")
      stopWatchProbe.expectTerminated(failingActor)
    }
  }

  private[this] def createActorAndSendMessage(response: HttpResponse): ActorRef = {
    And("a RecentMatches actor")
    val actorRef = system.actorOf(Props(new MockedMatchService(response)))

    When("recent matches are requested")
    actorRef.tell(GetRecentMatchIds("euw", 52477463, "1", "1", 20), probe.ref)

    actorRef
  }

  private[this] class MockedMatchService(httpResponse: HttpResponse) extends RecentMatchesService {
    override def riotGetRequest(regionParam: String, serviceName: String, queryParams: Map[String, String] = Map.empty,
                                prefix: String = "api/lol", hostType: String = "api") = Future(httpResponse)
  }

}
