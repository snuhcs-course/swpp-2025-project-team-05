package com.example.veato.data.local

import com.example.veato.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Local data source interface for user profiles
 */
interface ProfileLocalDataSource {
    /**
     * Save profile locally
     */
    suspend fun save(profile: UserProfile)

    /**
     * Get profile by user ID
     */
    suspend fun get(userId: String): UserProfile?

    /**
     * Get profile as Flow
     */
    fun getFlow(userId: String): Flow<UserProfile?>

    /**
     * Update existing profile
     */
    suspend fun update(profile: UserProfile)

    /**
     * Delete profile
     */
    suspend fun delete(userId: String)

    /**
     * Check if profile exists
     */
    suspend fun exists(userId: String): Boolean
}
