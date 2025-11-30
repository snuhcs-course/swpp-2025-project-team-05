package com.example.veato.data.model

data class Candidate(
    val name: String,
    val ranking: Int = 0,                  // LLM confidence ranking (lower is better)
    val voteCount: Int = 0,                // Number of votes received (Phase 2 or final results)
    val phase1ApprovalCount: Int = 0,      // Number of Phase 1 approvals (shown in Phase 2)
    val isRejected: Boolean = false        // Whether this candidate was rejected
)
