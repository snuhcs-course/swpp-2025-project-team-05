package com.example.veato.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class MealType(val displayName: String, val koreanName: String) {
    RICE_BASED("Rice-Based", "밥류"),
    NOODLE_BASED("Noodle-Based", "면류"),
    BREAD_BASED("Bread-Based", "빵류"),
    SOUP_BASED("Soup-Based", "국/찌개")
}
