package com.leagueprojecto.api

import com.leagueprojecto.api.domain._
import spray.json._

trait JsonProtocols extends DefaultJsonProtocol {
  implicit val matchFormat = jsonFormat(Match, "timestamp", "champion", "region", "queue", "season", "matchId", "role", "platformId", "lane")
  implicit val summonerFormat = jsonFormat5(Summoner.apply)
  implicit val playerStatsFormat = jsonFormat4(PlayerStats.apply)
  //implicit val teamFormat = jsonFormat1(Team.apply)

  // Riot Match related
  implicit val playerFormat = jsonFormat2(Player.apply)
  implicit val timeLineFormat = jsonFormat2(ParticipantTimeline.apply)
  implicit val participantStatsFormat = jsonFormat4(ParticipantStats.apply)
  implicit val participantIdentityFormat = jsonFormat2(ParticipantIdentity.apply)
  implicit val participantFormat = jsonFormat5(Participant.apply)
  implicit val riotMatchFormat = jsonFormat7(RiotMatch.apply)
}
