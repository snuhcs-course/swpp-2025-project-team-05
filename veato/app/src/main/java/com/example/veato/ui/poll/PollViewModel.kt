package com.example.veato.ui.poll

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veato.data.repository.PollRepository
import com.example.veato.data.repository.PollRepositoryImpl
import com.example.veato.ui.profile.ProfileState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class PollViewModel(
    private val repository: PollRepository,
    private val userId: String,
    private val pollId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(PollScreenState())
    val state: StateFlow<PollScreenState> = _state.asStateFlow()

    init {
        observePoll()
    }

    private fun loadOnce() {
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true) }
            try {
                val poll = repository.getPoll(pollId)
                val currentPhase = _state.value.poll?.phase
                val newPhase = poll.phase

                // If phase changed, reset voting state to allow voting in new phase
                if (currentPhase != null && currentPhase != newPhase) {
                    _state.update {
                        it.copy(
                            poll = poll,
                            isBusy = false,
                            voted = false,
                            selectedIndices = emptySet(),
                            rejectionUsed = false,
                            rejectedCandidateName = null,
                            newlyAddedCandidateName = null,
                            vetoAnimationTimestamp = 0L
                        )
                    }
                } else {
                    _state.update { it.copy(poll = poll, isBusy = false) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isBusy = false) }
            }
        }
    }

    fun observePoll() {
        viewModelScope.launch {
            // initial fetch
            loadOnce()
            while (true) {
                try {
                    val poll = repository.getPoll(pollId)
                    val currentPhase = _state.value.poll?.phase
                    val newPhase = poll.phase

                    // If phase changed, reset voting state to allow voting in new phase
                    if (currentPhase != null && currentPhase != newPhase) {
                        _state.update {
                            it.copy(
                                poll = poll,
                                isBusy = false,
                                voted = false,
                                selectedIndices = emptySet(),
                                rejectionUsed = false,
                                rejectedCandidateName = null,
                                newlyAddedCandidateName = null,
                                vetoAnimationTimestamp = 0L
                            )
                        }
                    } else {
                        // Detect if candidate list changed (someone else vetoed)
                        val currentCandidates = _state.value.poll?.candidates?.map { it.name } ?: emptyList()
                        val newCandidates = poll.candidates.map { it.name }

                        // If candidates changed and we're in Phase 1, show animation for all users
                        if (currentCandidates.isNotEmpty() &&
                            newCandidates != currentCandidates &&
                            poll.phase == com.example.veato.data.model.PollPhase.PHASE1) {

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
                                                            (currentState.voted || poll.hasCurrentUserLockedIn)

                            // If user's voted candidate was removed, revoke their vote on backend
                            if (wasVotedCandidateRemoved) {
                                viewModelScope.launch {
                                    try {
                                        repository.revokeBallot(pollId, userId)
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
                                poll.candidates.mapIndexedNotNull { index, candidate ->
                                    if (candidate.name in selectedNames) index else null
                                }.toSet()
                            }

                            _state.update {
                                it.copy(
                                    poll = poll,
                                    isBusy = false,
                                    newlyAddedCandidateName = addedCandidate,
                                    rejectedCandidateName = removedCandidate,
                                    rejectionUsed = removedCandidate != null,
                                    vetoAnimationTimestamp = System.currentTimeMillis(),
                                    // Unlock user if their voted candidate was removed
                                    voted = if (wasVotedCandidateRemoved) false else it.voted,
                                    // Update selections based on candidate list changes
                                    selectedIndices = updatedSelections
                                )
                            }
                        } else {
                            _state.update { it.copy(poll = poll, isBusy = false) }
                        }
                    }

                    if (poll.isOpen) {
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

    // if index not in selectedIndices, add.
    // if index already in selectedIndices, remove.
    fun modifySelectedIndices(index: Int) {
        val newSet = if (index in _state.value.selectedIndices)
            _state.value.selectedIndices - index
        else
            _state.value.selectedIndices + index

        _state.update { it.copy(selectedIndices = newSet) }
    }

    fun clearSelectedIndices() {
        _state.update { it.copy(selectedIndices = emptySet()) }
    }

    fun sendBallot() {
        viewModelScope.launch {
            setVoted(true)
            repository.sendBallot(pollId, userId, _state.value.selectedIndices.toList())
            // refresh poll after voting
            loadOnce()
        }
    }
    fun revokeBallot() {
        viewModelScope.launch {
            repository.revokeBallot(pollId, userId)
            clearSelectedIndices()
            setVoted(false)
            loadOnce()
        }
    }

    // Phase-specific methods

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

        // Immediately reject candidate and get replacement
        viewModelScope.launch {
            // Set loading state
            _state.update { it.copy(isVetoing = true, vetoError = null) }

            try {
                // Call API to reject and get updated poll with replacement
                val updatedPoll = repository.rejectCandidateImmediately(pollId, index)

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

    fun submitPhase1Vote() {
        viewModelScope.launch {
            try {
                val approvedIndices = _state.value.selectedIndices.toList()

                // Convert rejected candidate name back to current index
                val rejectedIndex = _state.value.rejectedCandidateName?.let { name ->
                    _state.value.poll?.candidates?.indexOfFirst { it.name == name }?.takeIf { it >= 0 }
                }

                repository.submitPhase1Vote(pollId, approvedIndices, rejectedIndex)

                // Mark as voted/locked in
                _state.update { it.copy(voted = true) }

                // Refresh poll state
                loadOnce()
            } catch (e: Exception) {
                // Handle error (can add error state if needed)
            }
        }
    }

    fun submitPhase2Vote() {
        viewModelScope.launch {
            try {
                // In Phase 2, selectedIndices should only have one element
                val selectedIndex = _state.value.selectedIndices.firstOrNull()
                    ?: throw Exception("No candidate selected")

                repository.submitPhase2Vote(pollId, selectedIndex)

                // Mark as voted/locked in
                _state.update { it.copy(voted = true) }

                // Refresh poll state
                loadOnce()
            } catch (e: Exception) {
                // Handle error (can add error state if needed)
            }
        }
    }

    // remove demo close; backend auto-closes when time is up

}