package com.example.veato.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class HeavinessLevel(val displayName: String, val description: String) {
    LIGHT("Light", "Salads, soups, light meals"),
    MEDIUM("Medium", "Balanced meals"),
    HEAVY("Heavy", "Fried, rich, filling meals")
}
