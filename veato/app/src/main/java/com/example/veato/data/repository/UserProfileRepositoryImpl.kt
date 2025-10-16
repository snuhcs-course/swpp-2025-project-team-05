package com.example.veato.data.repository

import com.example.veato.data.local.ProfileLocalDataSource
import com.example.veato.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Implementation of UserProfileRepository
 */
class UserProfileRepositoryImpl(
    private val localDataSource: ProfileLocalDataSource
) : UserProfileRepository {

    override suspend fun saveProfile(profile: UserProfile): Result<Unit> {
        return try {
            if (!profile.validate()) {
                Result.failure(IllegalArgumentException("Invalid profile data"))
            } else {
                localDataSource.save(profile)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProfile(userId: String): UserProfile? {
        return try {
            localDataSource.get(userId)
        } catch (e: Exception) {
            null
        }
    }

    override fun getProfileFlow(userId: String): Flow<UserProfile?> {
        return localDataSource.getFlow(userId)
    }

    override suspend fun updateProfile(profile: UserProfile): Result<Unit> {
        return try {
            if (!profile.validate()) {
                Result.failure(IllegalArgumentException("Invalid profile data"))
            } else {
                localDataSource.update(profile)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProfile(userId: String): Result<Unit> {
        return try {
            localDataSource.delete(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isOnboardingComplete(userId: String): Boolean {
        return try {
            val profile = localDataSource.get(userId)
            profile?.isOnboardingComplete ?: false
        } catch (e: Exception) {
            false
        }
    }
}
