package com.example.veato.data.model

import com.example.veato.R
import kotlinx.serialization.Serializable

@Serializable
enum class Allergen(val displayName: String) {
    SHELLFISH("Shellfish"),
    PEANUTS("Peanuts"),
    TREE_NUTS("Tree Nuts"),
    DAIRY("Dairy"),
    EGGS("Eggs"),
    SOY("Soy"),
    WHEAT("Wheat"),
    FISH("Fish"),
    SESAME("Sesame")
    // CUSTOM removed - not in requirements
}

/**
 * Returns the drawable resource ID for this allergen's icon
 */
fun Allergen.getIconResource(): Int {
    return when (this) {
        Allergen.SHELLFISH -> R.drawable.ic_allergy_shellfish
        Allergen.PEANUTS -> R.drawable.ic_allergy_peanuts
        Allergen.TREE_NUTS -> R.drawable.ic_allergy_tree_nuts
        Allergen.DAIRY -> R.drawable.ic_allergy_dairy
        Allergen.EGGS -> R.drawable.ic_allergy_eggs
        Allergen.SOY -> R.drawable.ic_allergy_soy
        Allergen.WHEAT -> R.drawable.ic_allergy_wheat
        Allergen.FISH -> R.drawable.ic_allergy_fish
        Allergen.SESAME -> R.drawable.ic_allergy_sesame
    }
}
