package com.leagueprojecto.api.services.riot

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.HttpResponse
import com.leagueprojecto.api.services.riot.MatchService.{GetMatch, MatchRetrievalFailed, Result}
import akka.http.scaladsl.model.StatusCodes._
import akka.testkit.TestProbe
import com.leagueprojecto.api.testHelpers.BaseServiceHelper

import scala.concurrent.Future

class MatchServiceSpec(_system: ActorSystem) extends BaseServiceHelper(_system) {

  val matchId = 2614000573L

  val probe: TestProbe = TestProbe()

  def this() = this(ActorSystem("MatchServiceSpec"))

  "MatchService" should {
    "return a Result containing a List of MatchHistories" in {
      Given("a valid response")
      val json = readFileFromClasspath("/responses/match/happyFlow.json")
      val response = HttpResponse(status = OK, entity = json)

      createActorAndSendMessage(response)

      Then("a Result object should be returned")
      val result: Result = probe.expectMsgClass(classOf[Result])

      And("the attributes should equal the json attributes")
      val matchDetail = result.matchDetail
      matchDetail.matchCreation shouldBe "1460574017402".toLong
      matchDetail.matchDuration shouldBe 2232
      matchDetail.queueType shouldBe "TEAM_BUILDER_DRAFT_RANKED_5x5"
      matchDetail.lane shouldBe "TOP"
      matchDetail.role shouldBe "SOLO"
      matchDetail.championId shouldBe 245
      matchDetail.winner shouldBe true
      matchDetail.stats.minionKills shouldBe 205
      matchDetail.stats.kills shouldBe 4
      matchDetail.stats.deaths shouldBe 7
      matchDetail.stats.assists shouldBe 9
    }
    "return a MatchRetrievalFailed containing the failed match id" in {
      Given("A bad response")
      val response = HttpResponse(status = BadRequest)

      val failingActor = createActorAndSendMessage(response)
      val stopWatchProbe = TestProbe()
      stopWatchProbe watch failingActor

      Then("a MatchRetrievalFailed containing the match id object should be returned")
      probe.expectMsg(MatchRetrievalFailed(matchId))

      And("the actor should be stopped")
      stopWatchProbe.expectTerminated(failingActor)
    }
  }

  private[this] def createActorAndSendMessage(response: HttpResponse): ActorRef = {
    And("a MatchService actor")
    val actorRef = system.actorOf(Props(new MockedMatchService(response)))

    When("match information is requested")
    actorRef.tell(GetMatch("euw", 52477463, matchId), probe.ref)

    actorRef
  }

  private[this] class MockedMatchService(httpResponse: HttpResponse) extends MatchService {
    override def riotGetRequest(regionParam: String, serviceName: String, queryParams: Map[String, String] = Map.empty,
                                prefix: String = "api/lol", hostType: String = "api") = Future(httpResponse)
  }
}
