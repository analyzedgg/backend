package com.leagueprojecto.api.domain

case class MatchHistory (
                        mapId: Int,
                        matchCreation: Long,
                        matchDuration: Long,
                        matchId: Long,
                        matchMode: String,
                        matchType: String,
                        matchVersion: String,
                        participantIdentities: List[ParticipantIdentity],
                        participants: List[Participant],
                        platformId: String,
                        queueType: String,
                        region: String,
                        season: String
                          )
