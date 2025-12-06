package com.example.veato.data.facade

import com.example.veato.data.repository.PollRepository
import com.example.veato.data.remote.StartPollRequest
import com.example.veato.data.remote.StartSessionResponse
import com.example.veato.ui.poll.mapper.PollMapper
import com.example.veato.ui.poll.model.PollUiModel

/**
 * Facade Pattern: Provides a simplified, high-level interface to the voting subsystem
 * This encapsulates the complexity of coordinating multiple repository calls and DTO mapping
 */
class VoteFlowFacade(
    private val pollRepository: PollRepository
) {
    
    /**
     * Start a new voting session
     * Returns UI model ready for consumption
     */
    suspend fun startPoll(
        teamId: String,
        pollTitle: String,
        durationMinutes: Int,
        includedMemberIds: List<String>,
        occasionNote: String
    ): StartSessionResponse {
        return pollRepository.startVotingSession(
            teamId = teamId,
            pollTitle = pollTitle,
            durationMinutes = durationMinutes,
            includedMemberIds = includedMemberIds,
            occasionNote = occasionNote
        )
    }
    
    /**
     * Get current poll state as UI model
     */
    suspend fun getPoll(pollId: String): PollUiModel {
        val poll = pollRepository.getPoll(pollId)
        return PollMapper.toUi(poll)
    }
    
    /**
     * Cast Phase 1 vote (approval + optional rejection)
     * Returns updated poll as UI model
     */
    suspend fun castPhase1Vote(
        pollId: String,
        approvedIndices: List<Int>,
        rejectedIndex: Int?
    ): PollUiModel {
        pollRepository.submitPhase1Vote(pollId, approvedIndices, rejectedIndex)
        return getPoll(pollId)
    }
    
    /**
     * Reject a candidate immediately (without locking in vote)
     * Returns updated poll as UI model
     */
    suspend fun rejectCandidateImmediately(
        pollId: String,
        rejectedIndex: Int
    ): PollUiModel {
        val poll = pollRepository.rejectCandidateImmediately(pollId, rejectedIndex)
        return PollMapper.toUi(poll)
    }
    
    /**
     * Cast Phase 2 vote (single selection)
     * Returns updated poll as UI model
     */
    suspend fun castPhase2Vote(
        pollId: String,
        selectedIndex: Int
    ): PollUiModel {
        pollRepository.submitPhase2Vote(pollId, selectedIndex)
        return getPoll(pollId)
    }
    
    /**
     * Revoke current user's ballot
     */
    suspend fun revokeBallot(pollId: String, userId: String) {
        pollRepository.revokeBallot(pollId, userId)
    }
}

