package com.example.veato.data.remote

import okhttp3.OkHttpClient
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.google.firebase.auth.FirebaseAuth

/**
 * Retrofit client configuration for API calls
 */
object RetrofitClient {

    // Local backend - for physical device (use computer's IP address)
    // private const val BASE_URL = "http://172.16.30.1:5001/"

    // Local backend - for emulator (uncomment if using emulator)
    // private const val BASE_URL = "http://10.0.2.2:5001/"

    // Production backend (uncomment when deploying)
    private const val BASE_URL = "https://veato-1.onrender.com/"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val authHeaderInterceptor = Interceptor { chain ->
        val original = chain.request()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "demo_user"
        val newReq = original.newBuilder()
            .header("X-User-Id", uid)
            .build()
        chain.proceed(newReq)
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authHeaderInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)  // Increased for Render cold starts
        .readTimeout(90, TimeUnit.SECONDS)     // Increased for LLM processing
        .writeTimeout(60, TimeUnit.SECONDS)    // Increased for larger payloads
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val pollApiService: PollApiService = retrofit.create(PollApiService::class.java)
}
