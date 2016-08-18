package com.analyzedgg.api.domain.riot

case class RiotRecentMatches (matches: Seq[RecentMatch])

case class RecentMatch(matchId: Long)
