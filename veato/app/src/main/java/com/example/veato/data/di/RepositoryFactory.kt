package com.example.veato.data.di

import android.content.Context
import com.example.veato.data.local.ProfileDataStoreImpl
import com.example.veato.data.local.ProfileLocalDataSource
import com.example.veato.data.remote.ProfileApiDataSource
import com.example.veato.data.remote.RetrofitClient
import com.example.veato.data.repository.PollRepository
import com.example.veato.data.repository.PollRepositoryImpl
import com.example.veato.data.repository.UserProfileRepository
import com.example.veato.data.repository.UserProfileRepositoryImpl

/**
 * Factory Method Pattern: Interface for creating repository instances
 * This allows easy substitution of implementations (e.g., for testing)
 */
interface RepositoryFactory {
    fun createUserProfileRepository(context: Context): UserProfileRepository
    fun createPollRepository(): PollRepository
}

/**
 * Default implementation of RepositoryFactory
 * Uses Singleton RetrofitClient and creates concrete repository implementations
 */
class DefaultRepositoryFactory : RepositoryFactory {
    
    override fun createUserProfileRepository(context: Context): UserProfileRepository {
        val localDataSource: ProfileLocalDataSource = ProfileDataStoreImpl(context)
        val remoteDataSource = ProfileApiDataSource() // Uses Firebase Firestore directly
        return UserProfileRepositoryImpl(localDataSource, remoteDataSource)
    }
    
    override fun createPollRepository(): PollRepository {
        return PollRepositoryImpl() // Uses RetrofitClient singleton internally
    }
}

