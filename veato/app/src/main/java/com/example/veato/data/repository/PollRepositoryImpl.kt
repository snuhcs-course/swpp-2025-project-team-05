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
    
    override suspend fun startVotingSession(teamId: String, pollTitle: String, durationMinutes: Int): StartSessionResponse {
        try {
            val request = StartPollRequest(teamId, pollTitle, durationMinutes)
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
                candidatesList.add(Candidate(candidateResponse.name))
            }
            
            // Parse results/ranking from backend response
            val resultsList = mutableListOf<Candidate>()
            pollResponse.resultRanking?.forEach { rankingResponse ->
                resultsList.add(Candidate(rankingResponse.name))
            }
            
            return Poll(
                pollId = pollResponse.pollId,
                pollTitle = pollResponse.pollTitle ?: "",
                startTime = Timestamp.now(), // We'll get actual timestamp from Firestore if needed
                duration = pollResponse.remainingSeconds ?: pollResponse.duration?.times(60) ?: 0,
                teamId = pollResponse.teamId ?: "",
                teamName = pollResponse.teamName ?: "",
                isOpen = pollResponse.status == "active",
                candidates = candidatesList,
                results = resultsList
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
}

