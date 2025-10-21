package com.example.veato.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class SpiceLevel(val level: Int, val displayName: String) {
    NONE(1, "No Spice/Mild Only"),
    LOW(2, "Low Spice Okay"),
    MEDIUM(3, "Medium Spice Preferred"),
    HIGH(4, "Spicy Lover"),
    EXTRA(5, "Extra Spicy");

    companion object {
        fun fromLevel(level: Int): SpiceLevel {
            return entries.find { it.level == level } ?: MEDIUM
        }
    }
}
