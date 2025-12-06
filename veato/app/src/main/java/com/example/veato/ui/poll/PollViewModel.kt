package com.example.veato.ui.poll

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veato.data.facade.VoteFlowFacade
import com.example.veato.ui.poll.model.PollPhaseUi
import com.example.veato.ui.poll.strategy.Phase1VotingStrategy
import com.example.veato.ui.poll.strategy.Phase2VotingStrategy
import com.example.veato.ui.poll.strategy.VotingPhaseStrategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * ViewModel for voting session
 * Uses Facade pattern (VoteFlowFacade) and Strategy pattern (VotingPhaseStrategy)
 */
class PollViewModel(
    private val facade: VoteFlowFacade,
    private val userId: String,
    private val pollId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(PollScreenState())
    val state: StateFlow<PollScreenState> = _state.asStateFlow()
    
    private var currentStrategy: VotingPhaseStrategy? = null

    init {
        observePoll()
    }

    private fun loadOnce() {
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true) }
            try {
                val pollUi = facade.getPoll(pollId)
                val currentPhase = _state.value.poll?.phase
                val newPhase = pollUi.phase

                // If phase changed, reset voting state and update strategy
                if (currentPhase != null && currentPhase != newPhase) {
                    updateStrategyForPhase(newPhase)
                    _state.update {
                        currentStrategy?.onPollLoaded(pollUi, it)?.copy(
                            isBusy = false,
                            voted = false,
                            selectedIndices = emptySet(),
                            rejectionUsed = false,
                            rejectedCandidateName = null,
                            newlyAddedCandidateName = null,
                            vetoAnimationTimestamp = 0L
                        ) ?: it.copy(poll = pollUi, isBusy = false)
                    }
                } else {
                    // Update strategy if not set
                    if (currentStrategy == null) {
                        updateStrategyForPhase(newPhase)
                    }
                    _state.update {
                        currentStrategy?.onPollLoaded(pollUi, it)?.copy(isBusy = false)
                            ?: it.copy(poll = pollUi, isBusy = false)
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isBusy = false) }
            }
        }
    }
    
    private fun updateStrategyForPhase(phase: PollPhaseUi) {
        currentStrategy = when (phase) {
            PollPhaseUi.PHASE1 -> Phase1VotingStrategy()
            PollPhaseUi.PHASE2 -> Phase2VotingStrategy()
            PollPhaseUi.CLOSED -> Phase1VotingStrategy() // Default strategy for closed polls
        }
    }

    fun observePoll() {
        viewModelScope.launch {
            // initial fetch
            loadOnce()
            while (true) {
                try {
                    val pollUi = facade.getPoll(pollId)
                    val currentPhase = _state.value.poll?.phase
                    val newPhase = pollUi.phase

                    // If phase changed, reset voting state to allow voting in new phase
                    if (currentPhase != null && currentPhase != newPhase) {
                        updateStrategyForPhase(newPhase)
                        _state.update {
                            currentStrategy?.onPollLoaded(pollUi, it)?.copy(
                                isBusy = false,
                                voted = false,
                                selectedIndices = emptySet(),
                                rejectionUsed = false,
                                rejectedCandidateName = null,
                                newlyAddedCandidateName = null,
                                vetoAnimationTimestamp = 0L
                            ) ?: it.copy(poll = pollUi, isBusy = false)
                        }
                    } else {
                        // Update strategy if not set
                        if (currentStrategy == null) {
                            updateStrategyForPhase(newPhase)
                        }
                        
                        // Detect if candidate list changed (someone else vetoed)
                        val currentCandidates = _state.value.poll?.candidates?.map { it.name } ?: emptyList()
                        val newCandidates = pollUi.candidates.map { it.name }

                        // If candidates changed and we're in Phase 1, show animation for all users
                        if (currentCandidates.isNotEmpty() &&
                            newCandidates != currentCandidates &&
                            pollUi.phase == PollPhaseUi.PHASE1) {

                            // Find which candidate was added (replacement)
                            val addedCandidate = newCandidates.firstOrNull { it !in currentCandidates }
                            // Find which candidate was removed (vetoed)
                            val removedCandidate = currentCandidates.firstOrNull { it !in newCandidates }

                            // IMPORTANT: Check if the removed candidate was in this user's locked selections
                            // If so, we need to unlock them so they can revote
                            val currentState = _state.value
                            val removedCandidateIndex = currentState.selectedIndices.find { index ->
                                val candidateName = currentState.poll?.candidates?.getOrNull(index)?.name
                                candidateName == removedCandidate
                            }
                            val wasVotedCandidateRemoved = removedCandidateIndex != null &&
                                                            (currentState.voted || pollUi.hasCurrentUserLockedIn)

                            // If user's voted candidate was removed, revoke their vote on backend
                            if (wasVotedCandidateRemoved) {
                                viewModelScope.launch {
                                    try {
                                        facade.revokeBallot(pollId, userId)
                                        // State will be updated in the next poll cycle
                                    } catch (e: Exception) {
                                        // Log but don't block - polling will retry
                                        android.util.Log.e("PollViewModel", "Failed to revoke ballot: ${e.message}")
                                    }
                                }
                            }

                            // Map old selections to new candidate list by name
                            val updatedSelections = if (wasVotedCandidateRemoved) {
                                // User voted for removed candidate - clear ALL selections so they revote properly
                                emptySet()
                            } else {
                                // User didn't vote for removed candidate - preserve their selections by name
                                val selectedNames = currentState.selectedIndices.mapNotNull { index ->
                                    currentState.poll?.candidates?.getOrNull(index)?.name
                                }
                                pollUi.candidates.mapIndexedNotNull { index, candidate ->
                                    if (candidate.name in selectedNames) index else null
                                }.toSet()
                            }

                            _state.update {
                                currentStrategy?.onPollLoaded(pollUi, it)?.copy(
                                    isBusy = false,
                                    newlyAddedCandidateName = addedCandidate,
                                    rejectedCandidateName = removedCandidate,
                                    rejectionUsed = removedCandidate != null,
                                    vetoAnimationTimestamp = System.currentTimeMillis(),
                                    // Unlock user if their voted candidate was removed
                                    voted = if (wasVotedCandidateRemoved) false else it.voted,
                                    // Update selections based on candidate list changes
                                    selectedIndices = updatedSelections
                                ) ?: it.copy(
                                    poll = pollUi,
                                    isBusy = false,
                                    newlyAddedCandidateName = addedCandidate,
                                    rejectedCandidateName = removedCandidate,
                                    rejectionUsed = removedCandidate != null,
                                    vetoAnimationTimestamp = System.currentTimeMillis(),
                                    voted = if (wasVotedCandidateRemoved) false else it.voted,
                                    selectedIndices = updatedSelections
                                )
                            }
                        } else {
                            _state.update {
                                currentStrategy?.onPollLoaded(pollUi, it)?.copy(isBusy = false)
                                    ?: it.copy(poll = pollUi, isBusy = false)
                            }
                        }
                    }

                    if (pollUi.isOpen) {
                        delay(1000)  // Reduced from 2000ms to 1000ms for faster sync
                    } else {
                        break
                    }
                } catch (e: Exception) {
                    delay(3000)
                }
            }
        }
    }

    fun setVoted(voted: Boolean) {
        _state.update { it.copy(voted = voted) }
    }

    // Use Strategy pattern for candidate selection
    fun modifySelectedIndices(index: Int) {
        val currentState = _state.value
        val poll = currentState.poll ?: return
        val strategy = currentStrategy ?: return
        
        val newState = strategy.onCandidateClicked(currentState, index, poll)
        _state.update { newState }
    }

    fun clearSelectedIndices() {
        _state.update { it.copy(selectedIndices = emptySet()) }
    }

    fun sendBallot() {
        viewModelScope.launch {
            setVoted(true)
            // Legacy method - kept for compatibility but should use phase-specific methods
            loadOnce()
        }
    }
    
    fun revokeBallot() {
        viewModelScope.launch {
            facade.revokeBallot(pollId, userId)
            clearSelectedIndices()
            setVoted(false)
            loadOnce()
        }
    }

    // Phase-specific methods using Strategy pattern

    fun setRejectedCandidate(index: Int?) {
        if (index == null) {
            // Clear rejection
            _state.update {
                it.copy(
                    rejectedCandidateName = null,
                    rejectionUsed = false,
                    vetoError = null,
                    newlyAddedCandidateName = null,
                    vetoAnimationTimestamp = 0L
                )
            }
            return
        }

        // Get candidate name from current poll
        val candidateName = _state.value.poll?.candidates?.getOrNull(index)?.name ?: return
        val currentCandidates = _state.value.poll?.candidates?.map { it.name } ?: return
        val wasSelectedCandidateVetoed = index in _state.value.selectedIndices

        // Immediately reject candidate and get replacement using Facade
        viewModelScope.launch {
            // Set loading state
            _state.update { it.copy(isVetoing = true, vetoError = null) }

            try {
                // Call Facade to reject and get updated poll with replacement
                val updatedPoll = facade.rejectCandidateImmediately(pollId, index)

                // Detect replacement candidate by comparing lists
                val updatedCandidates = updatedPoll.candidates.map { it.name }
                val newCandidateName = updatedCandidates.firstOrNull { it !in currentCandidates }

                // If user had selected the vetoed candidate, remove it from selections
                val updatedSelectedIndices = if (wasSelectedCandidateVetoed) {
                    _state.value.selectedIndices - index
                } else {
                    _state.value.selectedIndices
                }

                // Update state with new poll (includes replacement candidate)
                _state.update {
                    it.copy(
                        poll = updatedPoll,
                        rejectedCandidateName = candidateName,  // Store NAME not index
                        rejectionUsed = true,
                        isVetoing = false,
                        vetoError = null,
                        newlyAddedCandidateName = newCandidateName,
                        vetoAnimationTimestamp = System.currentTimeMillis(),
                        selectedIndices = updatedSelectedIndices
                    )
                }
            } catch (e: Exception) {
                // If rejection fails, show error and DON'T update local state
                _state.update {
                    it.copy(
                        isVetoing = false,
                        vetoError = e.message ?: "Failed to reject menu. Please try again."
                    )
                }
            }
        }
    }

    // Clear veto error (called when user dismisses error snackbar)
    fun clearVetoError() {
        _state.update { it.copy(vetoError = null) }
    }

    // Clear veto animation state (called when banner is dismissed)
    fun clearVetoAnimation() {
        _state.update {
            it.copy(
                newlyAddedCandidateName = null,
                vetoAnimationTimestamp = 0L
            )
        }
    }

    // Use Strategy pattern for Phase 1 vote submission
    fun submitPhase1Vote() {
        viewModelScope.launch {
            try {
                val currentState = _state.value
                val poll = currentState.poll ?: return@launch
                val strategy = currentStrategy as? Phase1VotingStrategy ?: return@launch

                // Use strategy to submit vote via facade
                val updatedPoll = strategy.submitVote(facade, poll, currentState)

                // Mark as voted/locked in
                _state.update { it.copy(voted = true, poll = updatedPoll) }

                // Refresh poll state
                loadOnce()
            } catch (e: Exception) {
                // Handle error (can add error state if needed)
            }
        }
    }

    // Use Strategy pattern for Phase 2 vote submission
    fun submitPhase2Vote() {
        viewModelScope.launch {
            try {
                val currentState = _state.value
                val poll = currentState.poll ?: return@launch
                val strategy = currentStrategy as? Phase2VotingStrategy ?: return@launch

                // Use strategy to submit vote via facade
                val updatedPoll = strategy.submitVote(facade, poll, currentState)

                // Mark as voted/locked in
                _state.update { it.copy(voted = true, poll = updatedPoll) }

                // Refresh poll state
                loadOnce()
            } catch (e: Exception) {
                // Handle error (can add error state if needed)
            }
        }
    }
}
