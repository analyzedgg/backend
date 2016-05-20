package com.leagueprojecto.api.domain

case class MatchDetail(
                        matchId: Long,
                        queueType: String,
                        matchDuration: Int,
                        matchCreation: Long,
                        summonerId: Long, // participantIdentities \ player \ summonerId
                        championId: Int, // participants
                        role: String, // participants \ timeline
                        lane: String, // participants \ timeline
                        winner: Boolean, // participants \ stats
                        stats: PlayerStats,
                        red: Team,
                        blue: Team
                      )
