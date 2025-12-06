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
        candidateIndex: Int,
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
        candidateIndex: Int,
        poll: PollUiModel
    ): PollScreenState {
        // Don't allow selecting a rejected candidate
        val candidateName = poll.candidates.getOrNull(candidateIndex)?.name
        if (currentState.rejectedCandidateName == candidateName) {
            return currentState
        }
        
        // Toggle selection (multi-select behavior)
        val newSet = if (candidateIndex in currentState.selectedIndices) {
            currentState.selectedIndices - candidateIndex
        } else {
            currentState.selectedIndices + candidateIndex
        }
        
        return currentState.copy(selectedIndices = newSet)
    }
    
    override suspend fun submitVote(
        facade: VoteFlowFacade,
        poll: PollUiModel,
        state: PollScreenState
    ): PollUiModel {
        // Convert rejected candidate name to index
        val rejectedIndex = state.rejectedCandidateName?.let { name ->
            poll.candidates.indexOfFirst { it.name == name }.takeIf { it >= 0 }
        }
        
        return facade.castPhase1Vote(
            pollId = poll.id,
            approvedIndices = state.selectedIndices.toList(),
            rejectedIndex = rejectedIndex
        )
    }
    
    override fun canSubmitVote(state: PollScreenState, poll: PollUiModel): Boolean {
        return !poll.hasCurrentUserLockedIn && 
               !state.voted && 
               state.selectedIndices.isNotEmpty()
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
        candidateIndex: Int,
        poll: PollUiModel
    ): PollScreenState {
        // Single-select behavior: clear previous and set new one
        return currentState.copy(
            selectedIndices = setOf(candidateIndex)
        )
    }
    
    override suspend fun submitVote(
        facade: VoteFlowFacade,
        poll: PollUiModel,
        state: PollScreenState
    ): PollUiModel {
        val selectedIndex = state.selectedIndices.firstOrNull()
            ?: throw IllegalStateException("No candidate selected for Phase 2")
        
        return facade.castPhase2Vote(
            pollId = poll.id,
            selectedIndex = selectedIndex
        )
    }
    
    override fun canSubmitVote(state: PollScreenState, poll: PollUiModel): Boolean {
        return !poll.hasCurrentUserLockedIn && 
               !state.voted && 
               state.selectedIndices.size == 1
    }
}

