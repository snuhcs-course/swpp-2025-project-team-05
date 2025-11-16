package com.example.veato.data.model

enum class PollPhase {
    PHASE1,
    PHASE2,
    CLOSED
}

data class Poll (
    val pollId: String = "",
    val pollTitle: String = "",
    val startTime: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now(),   // timestamp (millis)
    val duration: Int = 0,        // in seconds
    val teamId: String = "",
    val teamName: String = "",
    val isOpen: Boolean = false,
    val phase: PollPhase = PollPhase.PHASE1,
    val lockedInUserCount: Int = 0,
    val hasCurrentUserLockedIn: Boolean = false,
    val candidates: List<Candidate> = listOf(),
    val results: List<Candidate> = listOf()
)