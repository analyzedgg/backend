package com.leagueprojecto.api.domain

case class MatchHistory(
                         matchId: Long,
                         queueType: String,
                         matchDuration: Int,
                         matchCreation: Long,
                         stats: PlayerStats,
                         championId: Int,
                         role: String,
                         lane: String,
                         winner: Boolean
                         )
