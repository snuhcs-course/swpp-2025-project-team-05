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
                            rejectedCandidateName = null
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
                                rejectedCandidateName = null
                            )
                        }
                    } else {
                        _state.update { it.copy(poll = poll, isBusy = false) }
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
                    vetoError = null
                )
            }
            return
        }

        // Get candidate name from current poll
        val candidateName = _state.value.poll?.candidates?.getOrNull(index)?.name ?: return

        // Immediately reject candidate and get replacement
        viewModelScope.launch {
            // Set loading state
            _state.update { it.copy(isVetoing = true, vetoError = null) }

            try {
                // Call API to reject and get updated poll with replacement
                val updatedPoll = repository.rejectCandidateImmediately(pollId, index)

                // Update state with new poll (includes replacement candidate)
                _state.update {
                    it.copy(
                        poll = updatedPoll,
                        rejectedCandidateName = candidateName,  // Store NAME not index
                        rejectionUsed = true,
                        isVetoing = false,
                        vetoError = null
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