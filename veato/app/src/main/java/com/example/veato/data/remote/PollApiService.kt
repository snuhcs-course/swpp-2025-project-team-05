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
}

/**
 * Request data class for starting a poll
 */
data class StartPollRequest(
    val teamId: String,
    val pollTitle: String,
    val durationMinutes: Int
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
    val startedTime: String? = null,
    val duration: Int? = null,
    val remainingSeconds: Int? = null,
    val candidates: List<CandidateResponse>? = null,
    val yourCurrentVotes: List<String>? = null,
    val totalSelectedCountForYou: Int? = null,
    val resultRanking: List<RankingResponse>? = null,
    val winner: String? = null
)

/**
 * Response data class for poll candidates
 */
data class CandidateResponse(
    val name: String
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
