package com.example.veato.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

data class CheckEmailRequest(
    val email: String
)

data class CheckEmailResponse(
    val exists: Boolean? = null,
    val uid: String? = null,
    val error: String? = null
)

interface CheckEmailApiService {
    @POST("check-email")
    suspend fun checkEmail(@Body request: CheckEmailRequest): CheckEmailResponse
}
