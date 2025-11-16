package com.example.veato.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SoftPreferences(
    val favoriteCuisines: List<CuisineType> = emptyList(),
    val spiceTolerance: SpiceLevel = SpiceLevel.MEDIUM
) {
    /**
     * Check if any soft preferences are set beyond defaults
     */
    fun hasPreferences(): Boolean {
        return favoriteCuisines.isNotEmpty() ||
                spiceTolerance != SpiceLevel.MEDIUM
    }

    companion object {
        val DEFAULT = SoftPreferences()
    }
}
