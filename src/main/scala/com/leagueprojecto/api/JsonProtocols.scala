package com.leagueprojecto.api

import com.leagueprojecto.api.domain._
import spray.json._

trait JsonProtocols extends DefaultJsonProtocol {
  implicit val summonerFormat = jsonFormat5(Summoner.apply)
  implicit val playerStatsFormat = jsonFormat4(PlayerStats.apply)
  implicit val playerFormat = jsonFormat2(Player.apply)
  implicit val teamFormat = jsonFormat1(Team.apply)
  implicit val matchDetailFormat = jsonFormat12(MatchDetail.apply)
  implicit val matchlistFormat = jsonFormat9(Match.apply)
}
