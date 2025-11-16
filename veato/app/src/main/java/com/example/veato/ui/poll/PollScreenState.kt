package com.example.veato.ui.poll

import com.example.veato.data.model.Poll

data class PollScreenState(
    val poll: Poll? = null,
    val isBusy: Boolean = true,
    val voted: Boolean = false,
    val selectedIndices: Set<Int> = emptySet(),
    val rejectionUsed: Boolean = false,         // Whether user has rejected a candidate in Phase 1
    val rejectedCandidateIndex: Int? = null     // Index of rejected candidate (if any)
)
