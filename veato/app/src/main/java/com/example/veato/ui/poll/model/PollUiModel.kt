package com.example.veato.ui.poll.model

/**
 * UI Model for Poll - Adapter Pattern
 * This model is independent of backend DTO structure and optimized for UI consumption
 */
data class PollUiModel(
    val id: String,
    val title: String,
    val phase: PollPhaseUi,
    val candidates: List<CandidateUiModel>,
    val results: List<CandidateUiModel>,
    val teamId: String,
    val teamName: String,
    val isOpen: Boolean,
    val lockedInUserCount: Int,
    val hasCurrentUserLockedIn: Boolean,
    val remainingTimeSeconds: Long,
    val needsReview: Boolean = false,                     // User's vote was invalidated
    val invalidatedCandidates: List<String> = emptyList() // Candidates that were removed
)

/**
 * UI Model for Candidate
 */
data class CandidateUiModel(
    val name: String,
    val ranking: Int = 0,
    val voteCount: Int = 0,
    val phase1ApprovalCount: Int = 0,
    val isRejected: Boolean = false
)

/**
 * UI representation of Poll Phase
 */
enum class PollPhaseUi {
    PHASE1,
    PHASE2,
    CLOSED
}

