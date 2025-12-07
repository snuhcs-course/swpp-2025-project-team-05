package com.example.veato.ui.poll

import com.example.veato.ui.poll.model.PollUiModel

data class PollScreenState(
    val poll: PollUiModel? = null,
    val isBusy: Boolean = true,
    val voted: Boolean = false,
    val selectedCandidateNames: Set<String> = emptySet(),  // Changed from selectedIndices to names
    val rejectionUsed: Boolean = false,           // Whether user has rejected a candidate in Phase 1
    val rejectedCandidateName: String? = null,    // NAME of rejected candidate (not index, since list changes)
    val isVetoing: Boolean = false,               // Loading state while veto API call is in flight
    val vetoError: String? = null,                // Error message if veto fails
    val newlyAddedCandidateName: String? = null,  // NAME of replacement candidate for highlighting
    val vetoAnimationTimestamp: Long = 0L,        // Timestamp when veto completed for animation timing
    val needsReview: Boolean = false,             // User's vote was invalidated and needs review
    val invalidatedCandidateNames: List<String> = emptyList()  // Candidates that were removed from user's selections
)
