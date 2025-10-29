package com.example.veato.ui.poll

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.veato.data.repository.PollRepository

class PollViewModelFactory (
    private val repository: PollRepository,
    private val userId: String,
    private val pollId: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PollViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PollViewModel(repository, userId, pollId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}