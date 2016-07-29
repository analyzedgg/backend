package com.leagueprojecto.api

import com.leagueprojecto.api.domain._
import com.leagueprojecto.api.domain.riot._
import spray.json._

trait JsonProtocols extends DefaultJsonProtocol {
  //shared
  implicit val playerFormat = jsonFormat2(Player.apply)

  // Domain
  implicit val playerStatsFormat = jsonFormat4(PlayerStats.apply)
  implicit val teamFormat = jsonFormat1(Team.apply)
  implicit val teamsFormat = jsonFormat2(Teams.apply)
  implicit val matchDetailFormat = jsonFormat12(MatchDetail.apply)
  implicit val matchFormat = jsonFormat(Match, "timestamp", "champion", "region", "queue", "season", "matchId", "role", "platformId", "lane")
  implicit val summonerFormat = jsonFormat5(Summoner.apply)

  // Riot Match related
  implicit val timeLineFormat = jsonFormat2(ParticipantTimeline.apply)
  implicit val participantStatsFormat = jsonFormat5(ParticipantStats.apply)
  implicit val participantIdentityFormat = jsonFormat2(ParticipantIdentity.apply)
  implicit val participantFormat = jsonFormat5(Participant.apply)
  implicit val riotMatchFormat = jsonFormat7(RiotMatch.apply)
}
