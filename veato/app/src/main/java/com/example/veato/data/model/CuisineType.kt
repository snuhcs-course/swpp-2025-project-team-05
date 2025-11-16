package com.example.veato.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class CuisineType(val displayName: String, val koreanName: String) {
    KOREAN("Korean", "한식"),
    JAPANESE("Japanese", "일식"),
    CHINESE("Chinese", "중식"),
    WESTERN("Western", "양식"),
    INDIAN("Indian", "인도"),
    MEXICAN("Mexican", "멕시칸"),
    THAI("Thai", "태국"),
    SOUTHEAST_ASIAN("Southeast Asian", "동남아"),
    ITALIAN("Italian", "이탈리안"),
    FRENCH("French", "프랑스")
}
