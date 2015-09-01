package com.leagueprojecto.api.services.riot

import akka.actor.Status.Failure
import akka.actor.{ActorLogging, ActorRef, Props, Actor}
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpResponse, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.fasterxml.jackson.databind.ObjectMapper
import com.leagueprojecto.api.domain.{PlayerStats, MatchHistory}
import com.leagueprojecto.api.services.riot.SummonerService.SummonerNotFound
import io.gatling.jsonpath.JsonPath

object MatchHistoryService {

  case class GetMatchHistory(beginIndex: Int = 0, endIndex: Int = 15)

  case class MatchHistoryList(matches: List[MatchHistory])

  def props(region: String, summonerId: Long, queueType: String, championList: String) =
    Props(new MatchHistoryService(region, summonerId, queueType, championList))
}

class MatchHistoryService(regionParam: String, summonerId: Long, queueType: String, championList: String)
  extends Actor with ActorLogging with RiotService {

  import MatchHistoryService._

  override val region = regionParam
  override val service = matchHistoryBySummonerId + summonerId

  override def receive: Receive = {
    case GetMatchHistory(beginIndex, endIndex) =>
      implicit val origSender: ActorRef = sender()

      var queryParams = Map(
        "beginIndex" -> beginIndex.toString,
        "endIndex" -> endIndex.toString
      )

      if (queueType != "")    queryParams += "rankedQueues" -> queueType
      if (championList != "") queryParams += "championIds" -> championList

      val matchEndpoint: Uri = endpoint(queryParams)

      val future = riotRequest(RequestBuilding.Get(matchEndpoint))
      future onSuccess successHandler(origSender).orElse(defaultSuccessHandler(origSender))
      future onFailure failureHandler
  }

  def successHandler(origSender: ActorRef): PartialFunction[HttpResponse, Unit] = {
    case HttpResponse(OK, _, entity, _) =>
      Unmarshal(entity).to[String].onSuccess {
        case result: String =>
          val matches = transform(result)
          origSender ! MatchHistoryList(matches)
      }

    case HttpResponse(NotFound, _, _, _) =>
      val message = s"No summoner found by id '$summonerId' for region '$region'"
      log.warning(message)
      origSender ! Failure(new SummonerNotFound(message))
  }

  def failureHandler: PartialFunction[Throwable, Unit] = {
    case e: Exception =>
      println(s"request failed for some reason: ${e.getMessage}")
      e.printStackTrace()
  }

  private def transform(riotResult: String): List[MatchHistory] = {
    val jsonObject = (new ObjectMapper).readValue(riotResult, classOf[Object])

    val matches = JsonPath.query("$.matches[*]['queueType','matchDuration','matchCreation']", jsonObject).right.get.grouped(3).toList
    val participants = JsonPath.query("$.matches[*]['participants'][0]['championId']", jsonObject).right.get.toList
    val timeline = JsonPath.query("$.matches[*]['participants'][0]['timeline']['role','lane']", jsonObject).right.get.grouped(2).toList
    val stats = JsonPath.query("$.matches[*]['participants'][0]['stats']['minionsKilled','winner','kills','deaths','assists']", jsonObject).
      right.get.grouped(5).toList

    for (i <- matches.indices) yield
    MatchHistory(
      matches(i)(0).toString,
      matches(i)(1).toString.toInt,
      matches(i)(2).toString.toLong,
      PlayerStats(
        stats(i)(0).toString.toInt,
        stats(i)(2).toString.toInt,
        stats(i)(3).toString.toInt,
        stats(i)(4).toString.toInt
      ),
      participants(i).toString.toInt,
      timeline(i)(0).toString,
      timeline(i)(1).toString,
      stats(i)(1).toString.toBoolean
    )
  }.toList
}

