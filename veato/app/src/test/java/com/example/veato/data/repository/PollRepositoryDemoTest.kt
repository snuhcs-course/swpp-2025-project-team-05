package com.example.veato.data.repository

import com.example.veato.data.model.Candidate
import com.example.veato.data.model.Poll
import com.example.veato.data.remote.StartSessionResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PollRepositoryDemoTest {

    private val repo = PollRepositoryDemo()

    // --------------------------------------------------------------
    // startVotingSession
    // --------------------------------------------------------------
    @Test
    fun `startVotingSession returns demo session response`() = runTest {
        val response: StartSessionResponse = repo.startVotingSession(
            teamId = "team1",
            pollTitle = "Lunch",
            durationMinutes = 5,
            includedMemberIds = listOf("u1", "u2"),
            occasionNote = ""
        )

        assertEquals("demo_poll_123", response.pollId)
        assertEquals("Lunch", response.pollTitle)
        assertEquals(5, response.duration)
        assertTrue(response.candidates.contains("Pizza"))
    }

    // --------------------------------------------------------------
    // getPoll
    // --------------------------------------------------------------
    @Test
    fun `getPoll returns demo poll data`() = runTest {
        val poll: Poll = repo.getPoll("poll_demo")

        assertEquals("poll_demo", poll.pollId)
        assertEquals("What's for lunch?", poll.pollTitle)
        assertTrue(poll.isOpen)
        assertEquals(5, poll.candidates.size)
        assertEquals("Pizza", poll.candidates.first().name)
    }

    // --------------------------------------------------------------
    // sendBallot
    // --------------------------------------------------------------
    @Test
    fun `sendBallot does not throw`() = runTest {
        repo.sendBallot("poll_demo", "u123", listOf(1, 3))
    }

    // --------------------------------------------------------------
    // revokeBallot
    // --------------------------------------------------------------
    @Test
    fun `revokeBallot does not throw`() = runTest {
        repo.revokeBallot("poll_demo", "u123")
    }

    // --------------------------------------------------------------
    // submitPhase1Vote
    // --------------------------------------------------------------
    @Test
    fun `submitPhase1Vote does not throw`() = runTest {
        repo.submitPhase1Vote("poll_demo", listOf(0, 2), rejectedIndex = 3)
    }

    // --------------------------------------------------------------
    // submitPhase2Vote
    // --------------------------------------------------------------
    @Test
    fun `submitPhase2Vote does not throw`() = runTest {
        repo.submitPhase2Vote("poll_demo", 0)
    }
}
