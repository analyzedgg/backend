package com.leagueprojecto.api.domain

case class MatchHistory(
                         queueType: String,
                         matchDuration: Int,
                         stats: PlayerStats
                         )
