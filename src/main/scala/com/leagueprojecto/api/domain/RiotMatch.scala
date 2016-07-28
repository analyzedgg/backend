package com.leagueprojecto.api.domain


case class RiotMatch(
                      region: String,
                      queueType: String,
                      matchDuration: Long,
                      matchCreation: Long,
                      participantIdentities: Seq[ParticipantIdentity],
                      participants: Seq[Participant],
                      matchVersion: String
                    )

case class Participant(
                        participantId: Long,
                        championId: Long,
                        timeLine: TimeLine,
                        stats: ParticipantStats
                      )
case class ParticipantIdentity(
                              player: Player
                              )

case class Player(
                   summonerId: Long,
                   summonerName: String
                 )

case class TimeLine(
                     role: String,
                     lane: String
                   )

case class ParticipantStats(
                             assists: Long,
                             deaths: Long,
                             kills: Long,
                             minionsKilled: Long
                           )
