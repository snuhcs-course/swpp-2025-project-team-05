package com.example.veato.data.remote

import android.net.Uri
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

    /**
     * Uploads a profile image to Firebase Storage and returns the download URL
     */
    suspend fun uploadProfileImage(userId: String, imageUri: Uri): String
}