package com.leagueprojecto.api.services.riot

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.testkit.TestProbe
import com.leagueprojecto.api.domain.Summoner
import com.leagueprojecto.api.services.riot.RecentMatchesService.GetRecentMatchIds
import com.leagueprojecto.api.services.riot.SummonerService.{GetSummonerByName, Result, SummonerNotFound}
import com.leagueprojecto.api.testHelpers.BaseServiceHelper

import scala.concurrent.Future

class SummonerServiceSpec(_system: ActorSystem) extends BaseServiceHelper(_system) {
  val probe: TestProbe = TestProbe()

  def this() = this(ActorSystem("SummonerServiceTest"))

  "SummonerService" should {
    "return a Result object with a Summoner" in {
      Given("a valid response")
      val json = readFileFromClasspath("/responses/summoner/happyFlow.json")
      val response = HttpResponse(status = OK, entity = json)

      createActorAndSendMessage(response)

      Then("a Summoner object should be returned")
      val result = probe.expectMsgClass(classOf[Result])
      val summonerResult = result.summoner

      And("the name should equal 'Wagglez'")
      summonerResult.id shouldBe 52477463
      summonerResult.name shouldBe "Wagglez"
      summonerResult.profileIconId shouldBe 785
      summonerResult.summonerLevel shouldBe 30
      summonerResult.revisionDate shouldBe 1434315156000L
    }

    "return a Failure(SummonerNotFound) on a 404" in {
      Given("a 404 HTTP response")
      val response = HttpResponse(status = NotFound)

      createActorAndSendMessage(response)

      Then("a SummonerNotFound object should be returned")
      probe.expectMsg(SummonerNotFound)
    }

    "stop the actor on any other faulty response" in {
      Given("a bad response")
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
    actorRef.tell(GetSummonerByName("euw", "Wagglez"), probe.ref)

    actorRef
  }

  private[this] class MockedMatchService(httpResponse: HttpResponse) extends SummonerService {
    override def riotGetRequest(regionParam: String, serviceName: String, queryParams: Map[String, String] = Map.empty,
                                prefix: String = "api/lol", hostType: String = "api") = Future(httpResponse)
  }
}
