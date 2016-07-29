package com.leagueprojecto.api

import com.leagueprojecto.api.domain._
import com.leagueprojecto.api.services.riot.ChampionService.ChampionsResponse
import spray.json._

trait JsonProtocols extends DefaultJsonProtocol {
  implicit val summonerFormat = jsonFormat5(Summoner.apply)
  implicit val playerStatsFormat = jsonFormat4(PlayerStats.apply)
  implicit val playerFormat = jsonFormat2(Player.apply)
  implicit val teamFormat = jsonFormat1(Team.apply)
  implicit val teamsFormat = jsonFormat2(Teams.apply)
  implicit val matchDetailFormat = jsonFormat12(MatchDetail.apply)
  implicit val matchFormat = jsonFormat(Match, "timestamp", "champion", "region", "queue", "season", "matchId", "role", "platformId", "lane")
  implicit val championFormat = jsonFormat5(Champion.apply)
  implicit val championsResponseFormat = jsonFormat1(ChampionsResponse.apply)
}
