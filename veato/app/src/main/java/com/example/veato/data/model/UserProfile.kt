package com.example.veato.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class UserProfile(
    val userId: String = "",
    val userName: String = "",
    val fullName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val hardConstraints: HardConstraints = HardConstraints.EMPTY,
    val softPreferences: SoftPreferences = SoftPreferences.DEFAULT,
    val isOnboardingComplete: Boolean = false
) {
    /**
     * Convert profile to JSON string for storage
     */
    fun toJson(): String {
        return Json.encodeToString(this)
    }

    /**
     * Validate the entire profile
     */
    fun validate(): Boolean {
        // User ID must not be empty
        if (userId.isBlank()) {
            return false
        }

        // Validate hard constraints
        if (!hardConstraints.validate()) {
            return false
        }

        return true
    }

    /**
     * Create a copy with updated timestamp
     */
    fun withUpdatedTimestamp(): UserProfile {
        return copy(updatedAt = System.currentTimeMillis())
    }

    companion object {
        /**
         * Create UserProfile from JSON string
         */
        fun fromJson(json: String): UserProfile {
            return Json.decodeFromString(json)
        }

        /**
         * Create a new empty profile for a user
         */
        fun createNew(userId: String): UserProfile {
            return UserProfile(
                userId = userId,
                hardConstraints = HardConstraints.EMPTY,
                softPreferences = SoftPreferences.DEFAULT,
                isOnboardingComplete = false
            )
        }
    }
}
