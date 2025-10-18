package com.example.veato.data.remote

import com.example.veato.data.model.UserProfile

interface ProfileRemoteDataSource {

    /**
     * Syncs the local user profile and the remote data source.
     */
    suspend fun sync(profile: UserProfile)

    /**
     * Downloads the user profile from the remote data source.
     */
    suspend fun download(userId: String) : UserProfile?

    /**
     * Uploads the user profile to the remote data source.
     */
    suspend fun upload(profile: UserProfile)
}