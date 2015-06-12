package com.leagueprojecto.api.domain

case class MatchHistory(
                         queueType: String,
                         matchDuration: Int,
                         stats: PlayerStats,
                         championId: Int,
                         role: String,
                         lane: String,
                         winner: Boolean
                         )
