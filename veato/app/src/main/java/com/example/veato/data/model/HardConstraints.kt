package com.example.veato.data.model

import kotlinx.serialization.Serializable

@Serializable
data class HardConstraints(
    val dietaryRestrictions: List<DietaryType> = emptyList(),
    val allergies: List<Allergen> = emptyList(),
    val avoidIngredients: List<String> = emptyList()
) {
    /**
     * Check if any hard constraints are set
     */
    fun hasConstraints(): Boolean {
        return dietaryRestrictions.isNotEmpty() ||
                allergies.isNotEmpty() ||
                avoidIngredients.isNotEmpty()
    }

    /**
     * Validate the hard constraints
     */
    fun validate(): Boolean {
        return true
    }

    companion object {
        val EMPTY = HardConstraints()
    }
}
