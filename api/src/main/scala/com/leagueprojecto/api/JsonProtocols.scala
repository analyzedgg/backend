package com.leagueprojecto.api

import com.leagueprojecto.api.domain.{Match, PlayerStats, MatchDetail, Summoner}
import spray.json._

trait JsonProtocols extends DefaultJsonProtocol {
  implicit val summonerFormat = jsonFormat5(Summoner.apply)
  implicit val playerStatsFormat = jsonFormat4(PlayerStats.apply)
  implicit val matchDetailFormat = jsonFormat10(MatchDetail.apply)
  implicit val matchlistFormat = jsonFormat9(Match.apply)

}
