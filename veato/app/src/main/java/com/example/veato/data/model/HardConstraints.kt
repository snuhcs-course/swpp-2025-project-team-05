package com.example.veato.data.model

import kotlinx.serialization.Serializable

@Serializable
data class HardConstraints(
    val dietaryRestrictions: List<DietaryType> = emptyList(),
    val allergies: List<Allergen> = emptyList(),
    val avoidIngredients: List<String> = emptyList(),
    val budgetCap: Int? = null // Nullable - no hard cap if null
) {
    /**
     * Check if any hard constraints are set
     */
    fun hasConstraints(): Boolean {
        return dietaryRestrictions.isNotEmpty() ||
                allergies.isNotEmpty() ||
                avoidIngredients.isNotEmpty() ||
                budgetCap != null
    }

    /**
     * Validate the hard constraints
     */
    fun validate(): Boolean {
        // Budget cap must be positive if set
        if (budgetCap != null && budgetCap <= 0) {
            return false
        }
        return true
    }

    companion object {
        val EMPTY = HardConstraints()
    }
}
