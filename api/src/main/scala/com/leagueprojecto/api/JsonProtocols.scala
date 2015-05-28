package com.leagueprojecto.api

import com.leagueprojecto.api.domain.{PlayerStats, MatchHistory, Summoner}
import spray.json._

trait JsonProtocols extends DefaultJsonProtocol {
  implicit val summonerFormat = jsonFormat5(Summoner.apply)

  implicit val playerStatsFormat = jsonFormat1(PlayerStats.apply)
  implicit val matchHistoryFormat = jsonFormat3(MatchHistory.apply)
}
