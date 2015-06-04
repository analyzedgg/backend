package com.leagueprojecto.api

import com.leagueprojecto.api.domain.Summoner
import com.ning.http.client.AsyncHttpClient
import org.scalatest.{GivenWhenThen, Matchers, FlatSpec}
import spray.json._

class SummonerServiceTest extends FlatSpec with Matchers with GivenWhenThen with JsonProtocols {
  private final val HTTP_CLIENT: AsyncHttpClient  = new AsyncHttpClient
  private final val SERVER: String                = "http://localhost:8080"
  private final val API_ENDPOINT: String          = SERVER + "/api/EUW/summoner/"

  "The SummonerService" should "return a 200 response for an existing summoner" in {
    Given("an existing username")
    val username: String = "Wagglez"
    
    When("a request is sent")
    val future = HTTP_CLIENT.prepareGet(API_ENDPOINT + username).execute()

    Then("the response code should be OK")
    future.get().getStatusCode shouldBe 200
  }

  "The SummonerService" should "return a json body which can be mapped to a Summoner entity" in {
    Given("an existing username")
    val username: String = "Wagglez"

    And("a request is sent")
    val future = HTTP_CLIENT.prepareGet(API_ENDPOINT + username).execute()

    When("the response body is mapped on a summoner entity")
    val summoner = future.get().getResponseBody.parseJson.convertTo[Summoner]

    Then("summoner should be of type Summoner")
    summoner shouldBe an[Summoner]
  }

  "The SummonerService" should "return a 404 response for an non-existing summoner" in {
    Given("an existing username")
    val username: String = "Wagglez091283"

    When("a request is sent")
    val future = HTTP_CLIENT.prepareGet(API_ENDPOINT + username).execute()

    Then("the response code should be Not Found")
    future.get().getStatusCode shouldBe 404
  }
}
