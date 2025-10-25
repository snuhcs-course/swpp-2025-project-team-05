package com.example.veato.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class Allergen(val displayName: String) {
    SHELLFISH("Shellfish"),
    NUTS("Nuts (General)"),
    PEANUTS("Peanuts"),
    TREE_NUTS("Tree Nuts"),
    DAIRY("Dairy/Milk"),
    EGGS("Eggs"),
    SOY("Soy"),
    WHEAT("Wheat"),
    FISH("Fish"),
    SESAME("Sesame"),
    CUSTOM("Custom")
}
