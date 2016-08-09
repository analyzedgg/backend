package com.analyzedgg.api.domain

case class MatchDetail(
                        matchId: Long,
                        queueType: String,
                        matchDuration: Long,
                        matchCreation: Long,
                        summonerId: Long, // participantIdentities \ player \ summonerId
                        championId: Long, // participants
                        role: String, // participants \ timeline
                        lane: String, // participants \ timeline
                        winner: Boolean, // participants \ stats
                        matchVersion: String,
                        stats: PlayerStats,
                        teams: Teams
                      )
