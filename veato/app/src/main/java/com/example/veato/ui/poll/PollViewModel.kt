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
                            selectedCandidateNames = emptySet(),
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
                                selectedCandidateNames = emptySet(),
                                rejectionUsed = false,
                                rejectedCandidateName = null,
                                newlyAddedCandidateName = null,
                                vetoAnimationTimestamp = 0L,
                                needsReview = false,
                                invalidatedCandidateNames = emptyList()
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
                            val currentState = _state.value
                            val wasVotedCandidateRemoved = removedCandidate != null &&
                                                            removedCandidate in currentState.selectedCandidateNames &&
                                                            (currentState.voted || pollUi.hasCurrentUserLockedIn)

                            // Remove the vetoed candidate from selections (using names directly)
                            val updatedSelections = currentState.selectedCandidateNames - removedCandidate.orEmpty()

                            // Check if the removed candidate was the user's OWN rejected candidate
                            val isUserOwnRejection = currentState.rejectedCandidateName == removedCandidate
                            android.util.Log.d("PollViewModel", "Candidate removed: $removedCandidate, user's rejected: ${currentState.rejectedCandidateName}, isUserOwn: $isUserOwnRejection")

                            // Clear user's rejectedCandidateName if it matches the removed candidate
                            // (candidate no longer exists, so can't reject it)
                            val updatedRejectedCandidate = if (isUserOwnRejection) {
                                android.util.Log.d("PollViewModel", "Clearing user's rejectedCandidateName because it was removed")
                                null
                            } else {
                                currentState.rejectedCandidateName
                            }
                            val updatedRejectionUsed = updatedRejectedCandidate != null

                            // Only show VetoBanner animation if this was the user's own rejection
                            val shouldShowAnimation = isUserOwnRejection

                            // Set needsReview if vote was invalidated or backend indicates it
                            val needsReview = wasVotedCandidateRemoved || pollUi.needsReview

                            _state.update {
                                currentStrategy?.onPollLoaded(pollUi, it)?.copy(
                                    isBusy = false,
                                    newlyAddedCandidateName = if (shouldShowAnimation) addedCandidate else null,
                                    rejectedCandidateName = updatedRejectedCandidate,
                                    rejectionUsed = updatedRejectionUsed,
                                    vetoAnimationTimestamp = if (shouldShowAnimation) System.currentTimeMillis() else 0L,
                                    // Unlock user if their voted candidate was removed
                                    voted = if (needsReview) false else it.voted,
                                    // Update selections to remove vetoed candidate
                                    selectedCandidateNames = updatedSelections,
                                    // Set needs review flag
                                    needsReview = needsReview,
                                    invalidatedCandidateNames = if (needsReview) listOfNotNull(removedCandidate) else it.invalidatedCandidateNames
                                ) ?: it.copy(
                                    poll = pollUi,
                                    isBusy = false,
                                    newlyAddedCandidateName = if (shouldShowAnimation) addedCandidate else null,
                                    rejectedCandidateName = updatedRejectedCandidate,
                                    rejectionUsed = updatedRejectionUsed,
                                    vetoAnimationTimestamp = if (shouldShowAnimation) System.currentTimeMillis() else 0L,
                                    voted = if (needsReview) false else it.voted,
                                    selectedCandidateNames = updatedSelections,
                                    needsReview = needsReview,
                                    invalidatedCandidateNames = if (needsReview) listOfNotNull(removedCandidate) else it.invalidatedCandidateNames
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
    fun modifySelectedCandidates(candidateName: String) {
        val currentState = _state.value
        val poll = currentState.poll ?: return
        val strategy = currentStrategy ?: return

        val newState = strategy.onCandidateClicked(currentState, candidateName, poll)
        _state.update { newState }
    }

    fun clearSelectedCandidates() {
        _state.update { it.copy(selectedCandidateNames = emptySet()) }
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
            clearSelectedCandidates()
            setVoted(false)
            loadOnce()
        }
    }

    // Phase-specific methods using Strategy pattern

    fun setRejectedCandidate(candidateName: String?) {
        if (candidateName == null) {
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

        val currentCandidates = _state.value.poll?.candidates?.map { it.name } ?: return
        val wasSelectedCandidateVetoed = candidateName in _state.value.selectedCandidateNames

        // Immediately reject candidate and get replacement using Facade
        viewModelScope.launch {
            // Set loading state
            _state.update { it.copy(isVetoing = true, vetoError = null) }

            try {
                // Call Facade to reject and get updated poll with replacement
                val updatedPoll = facade.rejectCandidateImmediately(pollId, candidateName)

                // Detect replacement candidate by comparing lists
                val updatedCandidates = updatedPoll.candidates.map { it.name }
                val newCandidateName = updatedCandidates.firstOrNull { it !in currentCandidates }

                // If user had selected the vetoed candidate, remove it from selections
                val updatedSelectedCandidates = if (wasSelectedCandidateVetoed) {
                    _state.value.selectedCandidateNames - candidateName
                } else {
                    _state.value.selectedCandidateNames
                }

                // Update state with new poll (includes replacement candidate)
                _state.update {
                    it.copy(
                        poll = updatedPoll,
                        rejectedCandidateName = candidateName,  // Keep for VetoBanner display
                        rejectionUsed = true,
                        isVetoing = false,
                        vetoError = null,
                        newlyAddedCandidateName = newCandidateName,
                        vetoAnimationTimestamp = System.currentTimeMillis(),
                        selectedCandidateNames = updatedSelectedCandidates
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

    // Acknowledge needs review state (called when user dismisses needs review banner)
    fun acknowledgeReview() {
        _state.update {
            it.copy(
                needsReview = false,
                invalidatedCandidateNames = emptyList()
            )
        }
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
        android.util.Log.d("PollViewModel", "submitPhase1Vote called")
        viewModelScope.launch {
            try {
                val currentState = _state.value
                android.util.Log.d("PollViewModel", "Current state: selectedCandidates=${currentState.selectedCandidateNames}, rejectedCandidate=${currentState.rejectedCandidateName}")
                val poll = currentState.poll ?: run {
                    android.util.Log.e("PollViewModel", "Poll is null, cannot submit vote")
                    return@launch
                }
                val strategy = currentStrategy as? Phase1VotingStrategy ?: run {
                    android.util.Log.e("PollViewModel", "Strategy is not Phase1VotingStrategy, cannot submit vote")
                    return@launch
                }

                android.util.Log.d("PollViewModel", "Calling strategy.submitVote with pollId=${poll.id}")
                // Use strategy to submit vote via facade
                strategy.submitVote(facade, poll, currentState)
                android.util.Log.d("PollViewModel", "Vote submitted successfully")

                // Mark as voted/locked in
                _state.update { it.copy(voted = true) }

                // Refresh poll state - this will detect phase change if poll transitioned to Phase 2
                loadOnce()
            } catch (e: Exception) {
                // Handle error (can add error state if needed)
                android.util.Log.e("PollViewModel", "Error submitting Phase 1 vote: ${e.message}", e)
                e.printStackTrace()
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
                strategy.submitVote(facade, poll, currentState)

                // Mark as voted/locked in
                _state.update { it.copy(voted = true) }

                // Refresh poll state
                loadOnce()
            } catch (e: Exception) {
                // Handle error (can add error state if needed)
            }
        }
    }
}
