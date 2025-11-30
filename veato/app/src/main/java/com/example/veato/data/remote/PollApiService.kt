package com.example.veato.data.remote

import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit service interface for poll-related API calls
 */
interface PollApiService {

    @POST("polls/start")
    suspend fun startPoll(@Body request: StartPollRequest): Response<StartSessionResponse>

    @GET("polls/{pollId}")
    suspend fun getPoll(@Path("pollId") pollId: String): Response<PollResponse>

    @POST("polls/{pollId}/vote")
    suspend fun castVote(@Path("pollId") pollId: String, @Body request: VoteRequest): Response<VoteResponse>

    @POST("polls/{pollId}/phase1-vote")
    suspend fun castPhase1Vote(@Path("pollId") pollId: String, @Body request: Phase1VoteRequest): Response<VoteResponse>

    @POST("polls/{pollId}/phase2-vote")
    suspend fun castPhase2Vote(@Path("pollId") pollId: String, @Body request: Phase2VoteRequest): Response<VoteResponse>
}

/**
 * Request data class for starting a poll
 */
data class StartPollRequest(
    val teamId: String,
    val pollTitle: String,
    val durationMinutes: Int,
    val includedMemberIds: List<String>,
    val occasionNote: String = ""
)

/**
 * Response data class for starting a poll session
 */
data class StartSessionResponse(
    val pollId: String,
    val pollTitle: String,
    val teamName: String,
    val duration: Int,
    val startedTime: String,
    val candidates: List<String>
)

/**
 * Response data class for getting poll state
 */
data class PollResponse(
    val pollId: String,
    val pollTitle: String? = null,
    val teamId: String? = null,
    val teamName: String? = null,
    val status: String,
    val phase: String? = null,  // "phase1", "phase2", or "closed"
    val lockedInUserCount: Int? = null,
    val hasCurrentUserLockedIn: Boolean? = null,
    val startedTime: String? = null,
    val duration: Int? = null,
    val remainingSeconds: Int? = null,
    val candidates: List<CandidateResponse>? = null,
    val yourCurrentVotes: List<String>? = null,
    val totalSelectedCountForYou: Int? = null,
    val results: List<CandidateResponse>? = null,  // Results for closed polls (includes name + voteCount)
    val resultRanking: List<RankingResponse>? = null,  // Legacy field
    val winner: String? = null
)

/**
 * Response data class for poll candidates
 */
data class CandidateResponse(
    val name: String,
    val ranking: Int? = null,
    val voteCount: Int? = null,
    val phase1ApprovalCount: Int? = null,    // Phase 1 approval votes (shown in Phase 2)
    val isRejected: Boolean? = null
)

/**
 * Response data class for poll ranking results
 */
data class RankingResponse(
    val rank: Int,
    val name: String
)

/**
 * Request data class for casting a vote
 */
data class VoteRequest(
    val choices: List<String>
)

/**
 * Response data class for vote casting
 */
data class VoteResponse(
    val ok: Boolean,
    val yourCurrentVotes: List<String>,
    val totalSelectedCountForYou: Int
)

/**
 * Request data class for Phase 1 voting (approval + optional rejection)
 */
data class Phase1VoteRequest(
    val approvedCandidates: List<String>,
    val rejectedCandidate: String? = null,
    val lockIn: Boolean = true  // Set false to reject without locking in vote
)

/**
 * Request data class for Phase 2 voting (single selection)
 */
data class Phase2VoteRequest(
    val selectedCandidate: String
)
