package com.example.veato.ui.poll.strategy

import com.example.veato.data.facade.VoteFlowFacade
import com.example.veato.ui.poll.PollScreenState
import com.example.veato.ui.poll.model.PollUiModel

/**
 * Strategy Pattern: Interface for encapsulating phase-specific voting behavior
 * Allows interchangeable algorithms for Phase 1 vs Phase 2 voting logic
 */
interface VotingPhaseStrategy {
    
    /**
     * Initialize UI state when poll is loaded
     */
    fun onPollLoaded(poll: PollUiModel, currentState: PollScreenState): PollScreenState
    
    /**
     * Handle candidate selection click
     */
    fun onCandidateClicked(
        currentState: PollScreenState,
        candidateName: String,
        poll: PollUiModel
    ): PollScreenState
    
    /**
     * Submit vote using the appropriate phase-specific logic
     */
    suspend fun submitVote(
        facade: VoteFlowFacade,
        poll: PollUiModel,
        state: PollScreenState
    ): PollUiModel
    
    /**
     * Check if vote can be submitted (validation)
     */
    fun canSubmitVote(state: PollScreenState, poll: PollUiModel): Boolean
}

/**
 * Strategy for Phase 1: Multi-select approval voting with optional veto
 */
class Phase1VotingStrategy : VotingPhaseStrategy {
    
    override fun onPollLoaded(poll: PollUiModel, currentState: PollScreenState): PollScreenState {
        return currentState.copy(
            poll = poll,
            voted = poll.hasCurrentUserLockedIn
        )
    }
    
    override fun onCandidateClicked(
        currentState: PollScreenState,
        candidateName: String,
        poll: PollUiModel
    ): PollScreenState {
        // Don't allow selecting a rejected candidate
        if (currentState.rejectedCandidateName == candidateName) {
            return currentState
        }

        // Toggle selection (multi-select behavior using names)
        val newSet = if (candidateName in currentState.selectedCandidateNames) {
            currentState.selectedCandidateNames - candidateName
        } else {
            currentState.selectedCandidateNames + candidateName
        }

        return currentState.copy(selectedCandidateNames = newSet)
    }
    
    override suspend fun submitVote(
        facade: VoteFlowFacade,
        poll: PollUiModel,
        state: PollScreenState
    ): PollUiModel {
        // No conversion needed - already using names!
        return facade.castPhase1Vote(
            pollId = poll.id,
            approvedCandidateNames = state.selectedCandidateNames.toList(),
            rejectedCandidateName = state.rejectedCandidateName
        )
    }
    
    override fun canSubmitVote(state: PollScreenState, poll: PollUiModel): Boolean {
        return !poll.hasCurrentUserLockedIn &&
               !state.voted &&
               state.selectedCandidateNames.isNotEmpty() &&
               !state.needsReview  // Can't submit if needs review
    }
}

/**
 * Strategy for Phase 2: Single-choice voting
 */
class Phase2VotingStrategy : VotingPhaseStrategy {
    
    override fun onPollLoaded(poll: PollUiModel, currentState: PollScreenState): PollScreenState {
        return currentState.copy(
            poll = poll,
            voted = poll.hasCurrentUserLockedIn
        )
    }
    
    override fun onCandidateClicked(
        currentState: PollScreenState,
        candidateName: String,
        poll: PollUiModel
    ): PollScreenState {
        // Single-select behavior: clear previous and set new one
        return currentState.copy(
            selectedCandidateNames = setOf(candidateName)
        )
    }
    
    override suspend fun submitVote(
        facade: VoteFlowFacade,
        poll: PollUiModel,
        state: PollScreenState
    ): PollUiModel {
        val selectedCandidate = state.selectedCandidateNames.firstOrNull()
            ?: throw IllegalStateException("No candidate selected for Phase 2")

        return facade.castPhase2Vote(
            pollId = poll.id,
            selectedCandidateName = selectedCandidate
        )
    }
    
    override fun canSubmitVote(state: PollScreenState, poll: PollUiModel): Boolean {
        return !poll.hasCurrentUserLockedIn &&
               !state.voted &&
               state.selectedCandidateNames.size == 1
    }
}

