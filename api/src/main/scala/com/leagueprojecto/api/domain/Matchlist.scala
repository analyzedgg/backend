package com.leagueprojecto.api.domain

case class Matchlist(
                    timestamp: Long,
                    champion: Int,
                    region: String,
                    queue: String,
                    season: String,
                    matchId: Long,
                    role: String,
                    platformId: String,
                    lane: String
                      )
