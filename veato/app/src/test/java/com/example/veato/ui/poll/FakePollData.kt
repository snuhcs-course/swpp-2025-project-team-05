package com.example.veato.ui.poll

import com.example.veato.data.model.Candidate
import com.example.veato.data.model.Poll
import com.example.veato.data.model.PollPhase
import com.google.firebase.Timestamp

object FakePollData {

    fun examplePoll(): Poll {
        return Poll(
            pollId = "poll123",
            pollTitle = "Today's Lunch",
            startTime = Timestamp.now(),
            duration = 60,             // 60 seconds
            teamId = "team123",
            teamName = "Team Veato",
            isOpen = true,
            phase = PollPhase.PHASE1,
            lockedInUserCount = 0,
            hasCurrentUserLockedIn = false,
            candidates = listOf(
                Candidate(name = "Menu A"),
                Candidate(name = "Menu B")
            ),
            results = emptyList()
        )
    }
}
