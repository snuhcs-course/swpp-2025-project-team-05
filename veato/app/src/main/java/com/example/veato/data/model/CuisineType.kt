package com.example.veato.data.model

import com.example.veato.R
import kotlinx.serialization.Serializable

@Serializable
enum class CuisineType(val displayName: String, val koreanName: String) {
    KOREAN("Korean", "한식"),
    JAPANESE("Japanese", "일식"),
    CHINESE("Chinese", "중식"),
    WESTERN("Western", "양식"),
    EUROPEAN("Italian/French", "이탈리안/프랑스"),
    ASIAN("Southeast Asian", "동남아")
    // Legacy values removed: INDIAN, MEXICAN, THAI, ITALIAN, FRENCH
    // They map to: ASIAN (indian, thai), WESTERN (mexican), EUROPEAN (italian, french)
}

/**
 * Returns the drawable resource ID for this cuisine type's icon
 */
fun CuisineType.getIconResource(): Int {
    return when (this) {
        CuisineType.KOREAN -> R.drawable.ic_cuisine_korean
        CuisineType.JAPANESE -> R.drawable.ic_cuisine_japanese
        CuisineType.CHINESE -> R.drawable.ic_cuisine_chinese
        CuisineType.WESTERN -> R.drawable.ic_cuisine_western
        CuisineType.EUROPEAN -> R.drawable.ic_cuisine_european
        CuisineType.ASIAN -> R.drawable.ic_cuisine_asian
    }
}
