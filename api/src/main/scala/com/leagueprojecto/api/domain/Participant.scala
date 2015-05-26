package com.leagueprojecto.api.domain

case class Participant(
                        championId: Int,
                        highestAchievedSeasonTier: String,
                        masteries: List[Mastery],
                        partipantId: Int,
                        runes: List[Rune],
                        spell1Id: Int,
                        spell2Id: Int,
                        stats: ParticipantStats,
                        teamId: Int,
                        timeline: ParticipantTimeline
                        )