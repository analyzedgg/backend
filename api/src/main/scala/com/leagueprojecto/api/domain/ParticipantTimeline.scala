package com.leagueprojecto.api.domain

case class ParticipantTimeline(
                           ancientGolemAssistsPerMinCounts: ParticipantTimelineData,
                           ancientGolemKillsPerMinCounts: ParticipantTimelineData,
                           assistedLaneDeathsPerMinDeltas: ParticipantTimelineData,
                           assistedLaneKillsPerMinDeltas: ParticipantTimelineData,
                           baronAssistsPerMinCounts: ParticipantTimelineData,
                           baronKillsPerMinCounts: ParticipantTimelineData,
                           creepsPerMinDeltas: ParticipantTimelineData,
                           csDiffPerMinDeltas: ParticipantTimelineData,
                           damageTakenDiffPerMinDeltas: ParticipantTimelineData,
                           damageTakenPerMinDeltas: ParticipantTimelineData,
                           dragonAssistsPerMinCounts: ParticipantTimelineData,
                           dragonKillsPerMinCounts: ParticipantTimelineData,
                           elderLizardAssistsPerMinCounts: ParticipantTimelineData,
                           elderLizardKillsPerMinCounts: ParticipantTimelineData,
                           goldPerMinDeltas: ParticipantTimelineData,
                           inhibitorAssistsPerMinCounts: ParticipantTimelineData,
                           inhibitorKillsPerMinCounts: ParticipantTimelineData,
                           lane: String,
                           role: String
//                           towerAssistsPerMinCounts: ParticipantTimelineData,
//                           towerKillsPerMinCounts: ParticipantTimelineData,
//                           towerKillsPerMinDeltas: ParticipantTimelineData,
//                           vilemawAssistsPerMinCounts: ParticipantTimelineData,
//                           vilemawKillsPerMinCounts: ParticipantTimelineData,
//                           wardsPerMinDeltas: ParticipantTimelineData,
//                           xpDiffPerMinDeltas: ParticipantTimelineData,
//                           xpPerMinDeltas: ParticipantTimelineData
                           )
