package com.example.veato.data.remote

import android.util.Log
import com.example.veato.data.model.Allergen
import com.example.veato.data.model.CuisineType
import com.example.veato.data.model.DietaryType
import com.example.veato.data.model.HardConstraints
import com.example.veato.data.model.SoftPreferences
import com.example.veato.data.model.SpiceLevel
import com.example.veato.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await





class ProfileApiDataSource(
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ProfileRemoteDataSource {

    private val collection = firestore.collection("users")


    override suspend fun download(userId: String): UserProfile? {
        return try {
            val snapshot = collection.document(userId).get().await()
            if (snapshot.exists()) {
                val data = snapshot.data ?: return null

                val hardConstraints = HardConstraints(
                    dietaryRestrictions = (data["dietaryRestrictions"] as? List<String>)?.mapNotNull {
                        runCatching { DietaryType.valueOf(it) }.getOrNull()
                    } ?: emptyList(),

                    allergies = (data["allergies"] as? List<String>)?.mapNotNull {
                        runCatching { Allergen.valueOf(it) }.getOrNull()
                    } ?: emptyList()
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

                UserProfile(
                    userId = userId,
                    hardConstraints = hardConstraints,
                    softPreferences = softPreferences,
                    isOnboardingComplete = onboarding,
                    userName = username,
                    fullName = fullName,
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
                // Hard Constraints - convert sets to lists of strings
                "dietaryRestrictions" to profile.hardConstraints.dietaryRestrictions.map { it.name },
                "allergies" to profile.hardConstraints.allergies.map { it.name },
                "avoidIngredients" to profile.hardConstraints.avoidIngredients.toList(),
                "budgetCap" to profile.hardConstraints.budgetCap,
                // Soft Preferences - convert enums to strings
                "favoriteCuisines" to profile.softPreferences.favoriteCuisines.map { it.name },
                "spiceTolerance" to profile.softPreferences.spiceTolerance.name,
                "heavinessPreference" to profile.softPreferences.heavinessPreference.name,
                "typicalBudgetRange" to hashMapOf(
                    "minPrice" to profile.softPreferences.typicalBudgetRange.minPrice,
                    "maxPrice" to profile.softPreferences.typicalBudgetRange.maxPrice
                ),
                "mealTypePreferences" to profile.softPreferences.mealTypePreferences.map { it.name },
                "portionPreference" to (profile.softPreferences.portionPreference?.name ?: "MEDIUM")
            )

            collection.document(profile.userId)
                .set(profileData, com.google.firebase.firestore.SetOptions.merge())
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun sync(profile: UserProfile) {
        // download from Firestore
        // compare update Timestamp
        // upload or change local data
    }
}