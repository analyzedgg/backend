package com.leagueprojecto.api.services

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestProbe}
import akka.util.Timeout
import com.leagueprojecto.api.services.CacheService.CachedResponse
import org.scalatest.{GivenWhenThen, Matchers, FlatSpec}
import akka.pattern.ask
import scala.concurrent.Await
import scala.concurrent.duration._

class CacheServiceTest extends FlatSpec with Matchers with GivenWhenThen {
  implicit val actorSystem: ActorSystem = ActorSystem.create("API-test")
  implicit val timeout: Timeout = Timeout(1.second)
  implicit val executor = actorSystem.dispatcher

  val duration = Duration(1, SECONDS)

  trait Fixture {
    val targetProbe: TestProbe = TestProbe()
  }

  "Cache service" should "always return a CachedResponse object with a valid response" in new Fixture {
    Given("a CacheService")
    val cacheService = TestActorRef(CacheService.props[String](targetProbe.ref, 1000))

    And("a sample message and response")
    val message = "This is a sample message"
    val response = "This is a sample response"

    When("a message is sent")
    val future = cacheService ? message

    Then("the target probe should receive the message")
    targetProbe.expectMsg(message)
    targetProbe.reply(response)

    And("the type of the result should be a CachedResponse")
    val result = Await.result(future, duration)
    result shouldBe a[CachedResponse[_]]

    And("the response should be the same as the stubbed response")
    val cachedResponse = result.asInstanceOf[CachedResponse[String]]
    cachedResponse.response should equal(response)
  }

  it should "return the same response for two consequent calls" in new Fixture {
    Given("a CacheService")
    val cacheService = TestActorRef(CacheService.props[String](targetProbe.ref, 1000))

    And("one sample message and a response")
    val message = "Sample message"
    val response = "Sample response"

    When("the first message is sent")
    val future = cacheService ? message

    And("the target probe is stubbed with the response")
    targetProbe.expectMsg(message)
    targetProbe.reply(response)

    When("the second message is sent")
    val future2 = cacheService ? message

    Then("the target probe should receive no message")
    targetProbe.expectNoMsg(100.milliseconds)

    And("the cache invalidation times should be equal")
    val result1 = Await.result(future, duration).asInstanceOf[CachedResponse[String]]
    val result2 = Await.result(future2, duration).asInstanceOf[CachedResponse[String]]
    result1.cacheInvalidationDate shouldBe result2.cacheInvalidationDate
  }

  it should "remove the old cache upon getting a RemoveInvalidatedCache message" in new Fixture {
    true shouldBe false
    //fix this
  }
}
