package com.leagueprojecto.api.domain.riot

case class RiotMatch(
                      matchId: Long,
                      queueType: String,
                      matchDuration: Long,
                      matchCreation: Long,
                      participantIdentities: Seq[ParticipantIdentity],
                      participants: Seq[Participant],
                      matchVersion: String
                    )

case class Participant(
                        championId: Long,
                        participantId: Long,
                        teamId: Long,
                        timeline: ParticipantTimeline,
                        stats: ParticipantStats
                      )

case class ParticipantIdentity(
                                participantId: Long,
                                player: Player
                              )

case class Player(
                   summonerId: Long,
                   summonerName: String
                 )

case class ParticipantTimeline(
                                role: String,
                                lane: String
                              )

case class ParticipantStats(
                             assists: Long,
                             deaths: Long,
                             kills: Long,
                             minionsKilled: Long,
                             winner: Boolean
                           )