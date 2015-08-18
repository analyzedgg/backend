package com.leagueprojecto.api.services.riot

import akka.actor.{Props, ActorSystem}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.Flow
import com.leagueprojecto.api.services.riot.MatchHistoryService.{MatchHistoryList, GetMatchHistory}
import akka.http.scaladsl.model.StatusCodes._

class MatchHistoryServiceTest(_system: ActorSystem) extends BaseServiceTests(_system) {

  def this() = this(ActorSystem("MatchHistoryServiceTest"))

  "MatchHistoryService" should {
    "return a MatchHistoryList containing a List of MatchHistorys" in {
      Given("a valid response")
      val json = readFileFromClasspath("responses/matchhistory/happyflow.json")
      val response = HttpResponse(status = OK, entity = json)

      createActorAndSendMessage(response)

      Then("a MatchHistoryList object should be returned")
      val result = expectMsgClass(classOf[MatchHistoryList])

      And("the wrapped list should be of size 15")
      val list = result.matches
      list.size shouldBe 15

      And("the attributes should equal the json attributes")
      val first = list.head
      first.matchCreation shouldBe "1432328493438".toLong
      first.matchDuration shouldBe 1647
      first.queueType shouldBe "RANKED_SOLO_5x5"
      first.lane shouldBe "BOTTOM"
      first.role shouldBe "DUO_CARRY"
      first.championId shouldBe 22
      first.winner shouldBe true
      first.stats.minionKills shouldBe 225
      first.stats.kills shouldBe 5
      first.stats.deaths shouldBe 3
      first.stats.assists shouldBe 8
    }

    "return a MatchHistoryList containing an empty list" in {
      Given("an empty response")
      val json = readFileFromClasspath("responses/matchhistory/emptylist.json")
      val response = HttpResponse(status = OK, entity = json)

      createActorAndSendMessage(response)

      Then("a MatchHistoryList object should be returned")
      val result = expectMsgClass(classOf[MatchHistoryList])

      And("the wrapped list should be of empty")
      val list = result.matches
      list.size shouldBe 0
    }
  }

  def createActorAndSendMessage(response: HttpResponse): Unit = {
    And("a SummonerService actor")
    val actorRef = system.actorOf(Props(new MockedMatchHistoryService(response)))

    When("summoner information is requested")
    actorRef ! GetMatchHistory(0, 15)
  }

  class MockedMatchHistoryService(httpResponse: HttpResponse) extends MatchHistoryService("REGION", 123456798, "") {
    override lazy val riotConnectionFlow: Flow[HttpRequest, HttpResponse, Any] = Flow[HttpRequest].map { request =>
      httpResponse
    }
  }
}