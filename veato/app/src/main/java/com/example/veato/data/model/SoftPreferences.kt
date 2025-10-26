package com.example.veato.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SoftPreferences(
    val favoriteCuisines: List<CuisineType> = emptyList(),
    val spiceTolerance: SpiceLevel = SpiceLevel.MEDIUM,
    val mealTypePreferences: List<MealType> = emptyList(),
    val portionPreference: PortionSize? = null // Nullable - no preference if null
) {
    /**
     * Check if any soft preferences are set beyond defaults
     */
    fun hasPreferences(): Boolean {
        return favoriteCuisines.isNotEmpty() ||
                spiceTolerance != SpiceLevel.MEDIUM ||
                mealTypePreferences.isNotEmpty() ||
                portionPreference != null
    }

    companion object {
        val DEFAULT = SoftPreferences()
    }
}
