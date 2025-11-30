package com.example.veato.data.model

import com.example.veato.R
import kotlinx.serialization.Serializable

@Serializable
enum class DietaryType(val displayName: String) {
    VEGETARIAN("Vegetarian"),
    VEGAN("Vegan"),
    HALAL("Halal"),
    PESCATARIAN("Pescatarian"),
    GLUTEN_FREE("Gluten-Free"),
    LACTOSE_FREE("Lactose-Free")
    // KOSHER removed per new taxonomy
    // CUSTOM removed - not in requirements
}

/**
 * Returns the drawable resource ID for this dietary type's icon
 */
fun DietaryType.getIconResource(): Int {
    return when (this) {
        DietaryType.VEGETARIAN -> R.drawable.ic_dietary_vegetarian
        DietaryType.VEGAN -> R.drawable.ic_dietary_vegan
        DietaryType.HALAL -> R.drawable.ic_dietary_halal
        DietaryType.PESCATARIAN -> R.drawable.ic_dietary_pescatarian
        DietaryType.GLUTEN_FREE -> R.drawable.ic_dietary_gluten_free
        DietaryType.LACTOSE_FREE -> R.drawable.ic_dietary_lactose_free
    }
}
