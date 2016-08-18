package com.analyzedgg.api

import com.analyzedgg.api.domain._
import com.analyzedgg.api.domain.riot._
import com.analyzedgg.api.services.riot.ChampionService.ChampionsResponse
import spray.json._

import scala.concurrent.Future

trait JsonProtocols extends DefaultJsonProtocol {
  //shared
  implicit val playerFormat = jsonFormat2(Player.apply)

  // Domain
  implicit val playerStatsFormat = jsonFormat4(PlayerStats.apply)
  implicit val teamFormat = jsonFormat1(Team.apply)
  implicit val teamsFormat = jsonFormat2(Teams.apply)
  implicit val matchDetailFormat = jsonFormat12(MatchDetail.apply)
  implicit val matchFormat = jsonFormat(Match, "timestamp", "champion", "region", "queue", "season", "matchId", "role", "platformId", "lane")
  implicit val championFormat = jsonFormat5(Champion.apply)
  implicit val championListFormat = jsonFormat3(ChampionList.apply)
  implicit val championsResponseFormat = jsonFormat1(ChampionsResponse.apply)

  implicit val summonerFormat = jsonFormat5(Summoner.apply)

  // Riot Match related
  implicit val timeLineFormat = jsonFormat2(ParticipantTimeline.apply)
  implicit val participantStatsFormat = jsonFormat5(ParticipantStats.apply)
  implicit val participantIdentityFormat = jsonFormat2(ParticipantIdentity.apply)
  implicit val participantFormat = jsonFormat5(Participant.apply)
  implicit val riotMatchFormat = jsonFormat7(RiotMatch.apply)
}
