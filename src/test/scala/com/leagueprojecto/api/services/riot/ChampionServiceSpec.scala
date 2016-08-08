package com.leagueprojecto.api.services.riot

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.testkit.TestProbe
import com.leagueprojecto.api.services.riot.ChampionService.{ChampionsResponse, GetChampions}
import com.leagueprojecto.api.testHelpers.BaseServiceHelper

import scala.concurrent.Future

class ChampionServiceSpec(_system: ActorSystem) extends BaseServiceHelper(_system) {

  val probe: TestProbe = TestProbe()

  def this() = this(ActorSystem("ChampionServiceSpec"))

  "ChampionService" should {
    "return a ChampionsResponse containing a ChampionList" in {
      Given("a valid response")
      val json = readFileFromClasspath("/responses/champion/happyFlow.json")
      val response = HttpResponse(status = OK, entity = json)

      createActorAndSendMessage(response)

      Then("a ChampionsResponse object should be returned")
      val result: ChampionsResponse = probe.expectMsgClass(classOf[ChampionsResponse])

      And("the attributes should equal the json attributes")
      val championList = result.championList
      championList.`type` shouldBe "champion"
      championList.version shouldBe "6.15.1"
      championList.data.size shouldBe 131
      championList.data.head._1 shouldBe "Vayne"
    }
    "stop the ChampionService" in {
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
    And("a ChampionService actor")
    val actorRef = system.actorOf(Props(new MockedChampionService(response)))

    When("champion information is requested")
    actorRef.tell(GetChampions("euw"), probe.ref)

    actorRef
  }

  private[this] class MockedChampionService(httpResponse: HttpResponse) extends ChampionService {
    override def riotGetRequest(regionParam: String, serviceName: String, queryParams: Map[String, String] = Map.empty,
                                prefix: String = "api/lol", hostType: String = "api") = Future(httpResponse)
  }

}