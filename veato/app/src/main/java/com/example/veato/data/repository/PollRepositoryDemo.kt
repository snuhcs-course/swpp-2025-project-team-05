package com.example.veato.data.repository

import com.example.veato.data.model.Candidate
import com.example.veato.data.model.Poll

class PollRepositoryDemo : PollRepository{
    override suspend fun getPoll(pollId: String): Poll {
        return Poll(
            pollId = pollId,
            pollTitle = "What's for lunch?",
            startTime = com.google.firebase.Timestamp.now(),
            duration = 180,
            teamId = "dummy_team_abc",
            teamName = "The A-Team",
            isOpen = false,
            candidates = listOf(Candidate("Pizza"),Candidate("Sushi"),Candidate("Burger"),Candidate("Spaghetti"),Candidate("Bibimbap")),
            results = listOf(Candidate("Pizza"),Candidate("Sushi"),Candidate("Burger"),Candidate("Spaghetti"),Candidate("Bibimbap"))
        )
    }

    override suspend fun sendBallot(pollId: String, userId: String, selectedIndices: List<Int>) {
        // not yet implemented
    }

    override suspend fun revokeBallot(pollId: String, userId: String) {
        // not yet implemented
    }
}