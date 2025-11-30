package com.example.veato.ui.poll

import com.example.veato.data.model.Poll

data class PollScreenState(
    val poll: Poll? = null,
    val isBusy: Boolean = true,
    val voted: Boolean = false,
    val selectedIndices: Set<Int> = emptySet(),
    val rejectionUsed: Boolean = false,           // Whether user has rejected a candidate in Phase 1
    val rejectedCandidateName: String? = null,    // NAME of rejected candidate (not index, since list changes)
    val isVetoing: Boolean = false,               // Loading state while veto API call is in flight
    val vetoError: String? = null                 // Error message if veto fails
)
