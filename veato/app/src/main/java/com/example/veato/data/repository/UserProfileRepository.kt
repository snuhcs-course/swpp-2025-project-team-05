package com.example.veato.data.repository

import com.example.veato.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user profile operations
 */
interface UserProfileRepository {
    /**
     * Save a new user profile
     */
    suspend fun saveProfile(profile: UserProfile): Result<Unit>

    /**
     * Get user profile by user ID
     */
    suspend fun getProfile(userId: String): UserProfile?

    /**
     * Get user profile as a Flow for reactive updates
     */
    fun getProfileFlow(userId: String): Flow<UserProfile?>

    /**
     * Update an existing user profile
     */
    suspend fun updateProfile(profile: UserProfile): Result<Unit>

    /**
     * Delete user profile
     */
    suspend fun deleteProfile(userId: String): Result<Unit>

    /**
     * Check if onboarding is complete for a user
     */
    suspend fun isOnboardingComplete(userId: String): Boolean
}
