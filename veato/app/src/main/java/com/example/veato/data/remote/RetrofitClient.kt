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
    
    // Android emulator host mapping - 10.0.2.2 maps to host machine's localhost
    private const val BASE_URL = "http://10.0.2.2:5001/"
    
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
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val pollApiService: PollApiService = retrofit.create(PollApiService::class.java)
}
