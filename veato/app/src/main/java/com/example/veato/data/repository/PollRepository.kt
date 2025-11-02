package com.example.veato.data.repository

import com.example.veato.data.model.Poll
import com.example.veato.data.remote.StartSessionResponse

interface PollRepository {
    suspend fun getPoll(pollId: String): Poll
    suspend fun sendBallot(pollId: String, userId: String, selectedIndices: List<Int>)
    suspend fun revokeBallot(pollId: String, userId: String)
    suspend fun startVotingSession(teamId: String, pollTitle: String, durationMinutes: Int): StartSessionResponse
}