package com.example.veato.data.repository

import com.example.veato.data.model.Candidate
import com.example.veato.data.model.Poll
import com.example.veato.data.remote.StartSessionResponse

class PollRepositoryDemo : PollRepository{
    override suspend fun startVotingSession(
        teamId: String,
        pollTitle: String,
        durationMinutes: Int,
        includedMemberIds: List<String>,
        occasionNote: String
    ): StartSessionResponse {
        return StartSessionResponse(
            pollId = "demo_poll_123",
            pollTitle = pollTitle,
            teamName = "Demo Team",
            duration = durationMinutes,
            startedTime = "2025-10-26T12:00:00Z",
            candidates = listOf("Pizza", "Sushi", "Burger", "Spaghetti", "Bibimbap")
        )
    }

    override suspend fun getPoll(pollId: String): Poll {
        return Poll(
            pollId = pollId,
            pollTitle = "What's for lunch?",
            startTime = com.google.firebase.Timestamp.now(),
            duration = 180,
            teamId = "dummy_team_abc",
            teamName = "swpp team",
            isOpen = true,
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

    override suspend fun submitPhase1Vote(pollId: String, approvedIndices: List<Int>, rejectedIndex: Int?) {
        // Demo implementation - no-op
    }

    override suspend fun submitPhase2Vote(pollId: String, selectedIndex: Int) {
        // Demo implementation - no-op
    }
}