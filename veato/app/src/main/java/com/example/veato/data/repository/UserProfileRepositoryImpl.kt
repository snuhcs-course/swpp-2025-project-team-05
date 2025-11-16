package com.example.veato.data.repository

import android.net.Uri
import com.example.veato.data.local.ProfileLocalDataSource
import com.example.veato.data.model.UserProfile
import com.example.veato.data.remote.ProfileRemoteDataSource
import kotlinx.coroutines.flow.Flow

/**
 * Implementation of UserProfileRepository
 */
class UserProfileRepositoryImpl(
    private val localDataSource: ProfileLocalDataSource,
    private val remoteDataSource: ProfileRemoteDataSource
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
            val profile = remoteDataSource.download(userId)
            if(profile != null )localDataSource.update(profile)
            profile
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
                remoteDataSource.upload(profile)
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

    override suspend fun uploadProfileImage(userId: String, imageUri: Uri): String {
        return remoteDataSource.uploadProfileImage(userId, imageUri)
    }
}
