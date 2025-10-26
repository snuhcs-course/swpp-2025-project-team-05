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

class PollViewModel(
    private val repository: PollRepository,
    private val userId: String,
    private val pollId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(PollScreenState())
    val state: StateFlow<PollScreenState> = _state.asStateFlow()

    init {
        loadPoll()
    }

    fun loadPoll() {
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true) }

            try {
                val poll = repository.getPoll(pollId)
                _state.update { it.copy(poll= poll, isBusy = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isBusy = false) }
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
        }
    }
    fun revokeBallot() {
        viewModelScope.launch {
            repository.revokeBallot(pollId, userId)
            clearSelectedIndices()
            setVoted(false)
        }
    }

}