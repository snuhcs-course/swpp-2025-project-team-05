package com.example.veato.ui.poll.mapper

import com.example.veato.data.model.Candidate
import com.example.veato.data.model.Poll
import com.example.veato.data.model.PollPhase
import com.example.veato.ui.poll.model.CandidateUiModel
import com.example.veato.ui.poll.model.PollPhaseUi
import com.example.veato.ui.poll.model.PollUiModel

/**
 * Adapter Pattern: Converts backend Poll domain model to UI-friendly PollUiModel
 * This decouples UI from backend schema changes
 */
object PollMapper {
    
    /**
     * Convert domain Poll model to UI model
     */
    fun toUi(poll: Poll): PollUiModel {
        return PollUiModel(
            id = poll.pollId,
            title = poll.pollTitle,
            phase = mapPhase(poll.phase),
            candidates = poll.candidates.map { toCandidateUi(it) },
            results = poll.results.map { toCandidateUi(it) },
            teamId = poll.teamId,
            teamName = poll.teamName,
            isOpen = poll.isOpen,
            lockedInUserCount = poll.lockedInUserCount,
            hasCurrentUserLockedIn = poll.hasCurrentUserLockedIn,
            remainingTimeSeconds = poll.duration.toLong()
        )
    }
    
    private fun mapPhase(phase: PollPhase): PollPhaseUi {
        return when (phase) {
            PollPhase.PHASE1 -> PollPhaseUi.PHASE1
            PollPhase.PHASE2 -> PollPhaseUi.PHASE2
            PollPhase.CLOSED -> PollPhaseUi.CLOSED
        }
    }
    
    private fun toCandidateUi(candidate: Candidate): CandidateUiModel {
        return CandidateUiModel(
            name = candidate.name,
            ranking = candidate.ranking,
            voteCount = candidate.voteCount,
            phase1ApprovalCount = candidate.phase1ApprovalCount,
            isRejected = candidate.isRejected
        )
    }
}

