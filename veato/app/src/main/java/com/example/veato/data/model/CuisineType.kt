package com.example.veato.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class CuisineType(val displayName: String, val koreanName: String) {
    KOREAN("Korean", "한식"),
    CHINESE("Chinese", "중식"),
    JAPANESE("Japanese", "일식"),
    WESTERN("Western", "양식"),
    SNACKS("Snacks", "분식"),
    DESSERT("Dessert/Cafe", "디저트/카페"),
    SOUTHEAST_ASIAN("Southeast Asian", "동남아"),
    FUSION("Fusion", "퓨전")
}
