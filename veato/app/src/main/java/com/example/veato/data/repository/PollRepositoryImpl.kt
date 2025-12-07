package com.example.veato.data.repository

import android.util.Log
import com.example.veato.data.model.Candidate
import com.example.veato.data.model.Poll
import com.example.veato.data.remote.*
import com.google.firebase.Timestamp
import retrofit2.HttpException

/**
 * Implementation of PollRepository that uses backend API
 */
class PollRepositoryImpl : PollRepository {
    
    private val apiService = RetrofitClient.pollApiService
    
    override suspend fun startVotingSession(
        teamId: String,
        pollTitle: String,
        durationMinutes: Int,
        includedMemberIds: List<String>,
        occasionNote: String
    ): StartSessionResponse {
        try {
            val request = StartPollRequest(teamId, pollTitle, durationMinutes, includedMemberIds, occasionNote)
            val response = apiService.startPoll(request)
            
            if (response.isSuccessful) {
                return response.body() ?: throw Exception("Empty response body")
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                throw Exception("API call failed: ${response.code()} - $errorBody")
            }
        } catch (e: HttpException) {
            Log.e("PollRepository", "HTTP error starting poll: ${e.message}")
            throw Exception("Failed to start poll: ${e.message}")
        } catch (e: Exception) {
            Log.e("PollRepository", "Error starting poll: ${e.message}")
            throw e
        }
    }
    
    override suspend fun getPoll(pollId: String): Poll {
        try {
            // Call backend API to get poll state
            val response = apiService.getPoll(pollId)

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                throw Exception("API call failed: ${response.code()} - $errorBody")
            }

            val pollResponse = response.body() ?: throw Exception("Empty response body")

            // Parse candidates from backend response
            val candidatesList = mutableListOf<Candidate>()
            pollResponse.candidates?.forEach { candidateResponse ->
                candidatesList.add(
                    Candidate(
                        name = candidateResponse.name,
                        ranking = candidateResponse.ranking ?: 0,
                        voteCount = candidateResponse.voteCount ?: 0,
                        phase1ApprovalCount = candidateResponse.phase1ApprovalCount ?: 0,
                        isRejected = candidateResponse.isRejected ?: false
                    )
                )
            }

            // Parse results from backend response (for closed polls)
            val resultsList = mutableListOf<Candidate>()
            pollResponse.results?.forEach { resultResponse ->
                resultsList.add(
                    Candidate(
                        name = resultResponse.name,
                        voteCount = resultResponse.voteCount ?: 0
                    )
                )
            }

            // Parse phase from backend
            val phase = when (pollResponse.phase) {
                "phase1" -> com.example.veato.data.model.PollPhase.PHASE1
                "phase2" -> com.example.veato.data.model.PollPhase.PHASE2
                "closed" -> com.example.veato.data.model.PollPhase.CLOSED
                else -> if (pollResponse.status == "active") com.example.veato.data.model.PollPhase.PHASE1 else com.example.veato.data.model.PollPhase.CLOSED
            }

            return Poll(
                pollId = pollResponse.pollId,
                pollTitle = pollResponse.pollTitle ?: "",
                startTime = Timestamp.now(), // We'll get actual timestamp from Firestore if needed
                duration = pollResponse.remainingSeconds ?: pollResponse.duration?.times(60) ?: 0,
                teamId = pollResponse.teamId ?: "",
                teamName = pollResponse.teamName ?: "",
                isOpen = pollResponse.status == "active",
                phase = phase,
                lockedInUserCount = pollResponse.lockedInUserCount ?: 0,
                hasCurrentUserLockedIn = pollResponse.hasCurrentUserLockedIn ?: false,
                candidates = candidatesList,
                results = resultsList,
                needsReview = pollResponse.needsReview ?: false,
                invalidatedCandidates = pollResponse.invalidatedCandidates ?: emptyList()
            )
        } catch (e: HttpException) {
            Log.e("PollRepository", "HTTP error fetching poll: ${e.message}")
            throw Exception("Failed to fetch poll: ${e.message}")
        } catch (e: Exception) {
            Log.e("PollRepository", "Error fetching poll: ${e.message}")
            throw e
        }
    }
    
    override suspend fun sendBallot(pollId: String, userId: String, selectedIndices: List<Int>) {
        try {
            // First, get the current poll to get candidate names by index
            val poll = getPoll(pollId)
            
            if (!poll.isOpen) {
                throw Exception("Poll is not active")
            }
            
            // Convert indices to candidate names
            val selectedCandidates = selectedIndices.mapNotNull { index ->
                if (index in poll.candidates.indices) {
                    poll.candidates[index].name
                } else null
            }
            
            // Call backend API to cast vote
            val request = VoteRequest(selectedCandidates)
            val response = apiService.castVote(pollId, request)
            
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                throw Exception("API call failed: ${response.code()} - $errorBody")
            }
                
        } catch (e: HttpException) {
            Log.e("PollRepository", "HTTP error sending ballot: ${e.message}")
            throw Exception("Failed to send ballot: ${e.message}")
        } catch (e: Exception) {
            Log.e("PollRepository", "Error sending ballot: ${e.message}")
            throw e
        }
    }
    
    override suspend fun revokeBallot(pollId: String, userId: String) {
        try {
            // Call backend API to revoke vote (empty choices)
            val request = VoteRequest(emptyList())
            val response = apiService.castVote(pollId, request)

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                throw Exception("API call failed: ${response.code()} - $errorBody")
            }

        } catch (e: HttpException) {
            Log.e("PollRepository", "HTTP error revoking ballot: ${e.message}")
            throw Exception("Failed to revoke ballot: ${e.message}")
        } catch (e: Exception) {
            Log.e("PollRepository", "Error revoking ballot: ${e.message}")
            throw e
        }
    }

    override suspend fun submitPhase1Vote(pollId: String, approvedCandidateNames: List<String>, rejectedCandidateName: String?) {
        try {
            Log.d("PollRepository", "submitPhase1Vote called with pollId=$pollId, approved=$approvedCandidateNames, rejected=$rejectedCandidateName")

            // Get current poll to validate phase
            val poll = getPoll(pollId)
            Log.d("PollRepository", "Current poll phase: ${poll.phase}")

            if (poll.phase != com.example.veato.data.model.PollPhase.PHASE1) {
                throw Exception("Poll is not in Phase 1")
            }

            // No conversion needed - already using names!
            // Call backend API (with lockIn = true by default)
            val request = Phase1VoteRequest(approvedCandidateNames, rejectedCandidateName, lockIn = true)
            Log.d("PollRepository", "Calling API with request: $request")
            val response = apiService.castPhase1Vote(pollId, request)
            Log.d("PollRepository", "API response code: ${response.code()}, successful: ${response.isSuccessful}")

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("PollRepository", "API error: ${response.code()} - $errorBody")
                throw Exception("API call failed: ${response.code()} - $errorBody")
            }

            Log.d("PollRepository", "Phase 1 vote submitted successfully")

        } catch (e: HttpException) {
            Log.e("PollRepository", "HTTP error submitting Phase 1 vote: ${e.message}", e)
            throw Exception("Failed to submit Phase 1 vote: ${e.message}")
        } catch (e: Exception) {
            Log.e("PollRepository", "Error submitting Phase 1 vote: ${e.message}", e)
            throw e
        }
    }

    override suspend fun rejectCandidateImmediately(pollId: String, rejectedCandidateName: String): Poll {
        try {
            // Get current poll to validate phase
            val poll = getPoll(pollId)

            if (poll.phase != com.example.veato.data.model.PollPhase.PHASE1) {
                throw Exception("Poll is not in Phase 1")
            }

            // No conversion needed - already using name!
            // Call backend API with lockIn = false to reject without locking in
            val request = Phase1VoteRequest(emptyList(), rejectedCandidateName, lockIn = false)
            val response = apiService.castPhase1Vote(pollId, request)

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                throw Exception("API call failed: ${response.code()} - $errorBody")
            }

            // Fetch updated poll state to get the replacement candidate
            return getPoll(pollId)

        } catch (e: HttpException) {
            Log.e("PollRepository", "HTTP error rejecting candidate: ${e.message}")
            throw Exception("Failed to reject candidate: ${e.message}")
        } catch (e: Exception) {
            Log.e("PollRepository", "Error rejecting candidate: ${e.message}")
            throw e
        }
    }

    override suspend fun submitPhase2Vote(pollId: String, selectedCandidateName: String) {
        try {
            // Get current poll to validate phase
            val poll = getPoll(pollId)

            if (poll.phase != com.example.veato.data.model.PollPhase.PHASE2) {
                throw Exception("Poll is not in Phase 2")
            }

            // No conversion needed - already using name!
            // Call backend API
            val request = Phase2VoteRequest(selectedCandidateName)
            val response = apiService.castPhase2Vote(pollId, request)

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                throw Exception("API call failed: ${response.code()} - $errorBody")
            }

        } catch (e: HttpException) {
            Log.e("PollRepository", "HTTP error submitting Phase 2 vote: ${e.message}")
            throw Exception("Failed to submit Phase 2 vote: ${e.message}")
        } catch (e: Exception) {
            Log.e("PollRepository", "Error submitting Phase 2 vote: ${e.message}")
            throw e
        }
    }
}

