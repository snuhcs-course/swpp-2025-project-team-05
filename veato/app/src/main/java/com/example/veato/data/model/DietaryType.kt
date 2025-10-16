package com.example.veato.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class DietaryType(val displayName: String) {
    VEGETARIAN("Vegetarian"),
    VEGAN("Vegan"),
    HALAL("Halal"),
    KOSHER("Kosher"),
    PESCATARIAN("Pescatarian"),
    GLUTEN_FREE("Gluten-Free"),
    LACTOSE_FREE("Lactose-Free"),
    CUSTOM("Custom")
}
