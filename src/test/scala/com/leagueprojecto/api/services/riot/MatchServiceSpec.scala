package com.analyzedgg.api.services.riot

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.analyzedgg.api.services.riot.MatchService.{GetMatch, Result}
import akka.http.scaladsl.model.StatusCodes._
import akka.testkit.TestProbe
import com.analyzedgg.api.testHelpers.BaseServiceHelper

import scala.concurrent.Future

class MatchServiceSpec(_system: ActorSystem) extends BaseServiceHelper(_system) {

  val probe: TestProbe = TestProbe()

  def this() = this(ActorSystem("MatchServiceSpec"))

  "MatchService" should {
    "return a Result containing a List of MatchHistorys" in {
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

  }

  def createActorAndSendMessage(response: HttpResponse): Unit = {
    And("a SummonerService actor")
    val actorRef = system.actorOf(Props(new MockedMatchService(response)))

    When("summoner information is requested")
    actorRef.tell(GetMatch("euw", 52477463, 2614000573L), probe.ref)
  }

  class MockedMatchService(httpResponse: HttpResponse) extends MatchService {
    override def riotGetRequest(regionParam: String, serviceName: String, queryParams: Map[String, String] = Map.empty,
                                prefix: String = "api/lol", hostType: String = "api") = Future(httpResponse)
  }
}
