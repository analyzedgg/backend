package com.leagueprojecto.api.domain.riot

case class RiotRecentMatches (matches: Seq[RecentMatch])

case class RecentMatch(matchId: Long)
