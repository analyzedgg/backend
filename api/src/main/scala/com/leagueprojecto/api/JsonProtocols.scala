package com.leagueprojecto.api

import com.leagueprojecto.api.domain.{Matchlist, PlayerStats, MatchHistory, Summoner}
import spray.json._

trait JsonProtocols extends DefaultJsonProtocol {
  implicit val summonerFormat = jsonFormat5(Summoner.apply)
  implicit val playerStatsFormat = jsonFormat4(PlayerStats.apply)
  implicit val matchHistoryFormat = jsonFormat8(MatchHistory.apply)
  implicit val matchlistFormat = jsonFormat9(Matchlist.apply)

}
