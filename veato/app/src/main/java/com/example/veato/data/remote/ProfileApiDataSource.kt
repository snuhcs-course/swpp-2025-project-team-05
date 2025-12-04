package com.example.veato.data.remote

import android.net.Uri
import android.util.Log
import com.example.veato.data.model.Allergen
import com.example.veato.data.model.CuisineType
import com.example.veato.data.model.DietaryType
import com.example.veato.data.model.HardConstraints
import com.example.veato.data.model.SoftPreferences
import com.example.veato.data.model.SpiceLevel
import com.example.veato.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout





class ProfileApiDataSource(
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) : ProfileRemoteDataSource {

    private val collection = firestore.collection("users")
    private val storageRef = storage.reference


    override suspend fun download(userId: String): UserProfile? {
        return try {
            // Add timeout to prevent hanging on network issues
            val snapshot = withTimeout(5000L) { // 5 second timeout
                collection.document(userId).get().await()
            }
            if (snapshot.exists()) {
                val data = snapshot.data ?: return null

                val hardConstraints = HardConstraints(
                    dietaryRestrictions = (data["dietaryRestrictions"] as? List<String>)?.mapNotNull {
                        runCatching { DietaryType.valueOf(it) }.getOrNull()
                    } ?: emptyList(),

                    allergies = (data["allergies"] as? List<String>)?.mapNotNull {
                        runCatching { Allergen.valueOf(it) }.getOrNull()
                    } ?: emptyList(),

                    avoidIngredients = (data["avoidIngredients"] as? List<String>) ?: emptyList()
                )

                val softPreferences = SoftPreferences(
                    favoriteCuisines = (data["favoriteCuisines"] as? List<String>)?.mapNotNull {
                        runCatching { CuisineType.valueOf(it) }.getOrNull()
                    } ?: emptyList(),

                    spiceTolerance = runCatching {
                        SpiceLevel.valueOf(data["spiceTolerance"] as? String ?: "MEDIUM")
                    }.getOrDefault(SpiceLevel.MEDIUM),

                    )

                val onboarding = data["onboardingCompleted"] as? Boolean ?: false
                val fullName = data["fullName"] as? String ?: "full_name"
                val username = data["username"] as? String ?: "user_name"
                val profilePictureUrl = data["profilePictureUrl"] as? String ?: ""

                UserProfile(
                    userId = userId,
                    hardConstraints = hardConstraints,
                    softPreferences = softPreferences,
                    isOnboardingComplete = onboarding,
                    userName = username,
                    fullName = fullName,
                    profilePictureUrl = profilePictureUrl
                )
            } else {
                Log.d("ProfileApiDataSource", "No such document")

                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun upload(profile: UserProfile) {
        try {
            // Convert profile to Firebase-friendly map
            // Convert enums to strings and sets to lists
            val profileData = hashMapOf(
                "onboardingCompleted" to true,
                "lastUpdated" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                // User Info
                "fullName" to profile.fullName,
                "username" to profile.userName,
                "profilePictureUrl" to profile.profilePictureUrl,
                // Hard Constraints - convert sets to lists of strings
                "dietaryRestrictions" to profile.hardConstraints.dietaryRestrictions.map { it.name },
                "allergies" to profile.hardConstraints.allergies.map { it.name },
                "avoidIngredients" to profile.hardConstraints.avoidIngredients.toList(),
                // Soft Preferences - convert enums to strings
                "favoriteCuisines" to profile.softPreferences.favoriteCuisines.map { it.name },
                "spiceTolerance" to profile.softPreferences.spiceTolerance.name
            )

            // Add timeout to prevent hanging on network issues
            withTimeout(5000L) { // 5 second timeout
                collection.document(profile.userId)
                    .set(profileData, com.google.firebase.firestore.SetOptions.merge())
                    .await()
            }

            Log.d("ProfileApiDataSource", "Profile uploaded successfully")
        } catch (e: Exception) {
            Log.w("ProfileApiDataSource", "Failed to upload profile (offline or network error)", e)
            // Don't throw - allow offline usage
        }
    }

    override suspend fun uploadProfileImage(userId: String, imageUri: Uri): String {
        return try {
            // Create a unique filename with timestamp
            val timestamp = System.currentTimeMillis()
            val filename = "profile_images/${userId}_${timestamp}.jpg"
            val imageRef = storageRef.child(filename)

            // Upload the image
            imageRef.putFile(imageUri).await()

            // Get the download URL
            val downloadUrl = imageRef.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            Log.e("ProfileApiDataSource", "Error uploading profile image", e)
            throw e
        }
    }

    override suspend fun sync(profile: UserProfile) {
        // download from Firestore
        // compare update Timestamp
        // upload or change local data
    }
}