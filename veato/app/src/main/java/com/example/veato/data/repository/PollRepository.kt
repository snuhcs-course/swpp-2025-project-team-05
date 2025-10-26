package com.example.veato.data.repository

import com.example.veato.data.model.Poll

interface PollRepository {
    suspend fun getPoll(pollId: String): Poll
    suspend fun sendBallot(pollId: String, userId: String, selectedIndices: List<Int>)
    suspend fun revokeBallot(pollId: String, userId: String)
}