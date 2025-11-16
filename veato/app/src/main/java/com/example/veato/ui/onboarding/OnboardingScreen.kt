package com.example.veato.ui.onboarding

/**
 * Sealed class representing all onboarding screens
 */
sealed class OnboardingScreen(
    val route: String,
    val stepNumber: Int,
    val title: String,
    val description: String,
    val isSkippable: Boolean = false
) {
    data object Welcome : OnboardingScreen(
        route = "welcome",
        stepNumber = 0,
        title = "Welcome to Veato!",
        description = "Let's personalize your food recommendations",
        isSkippable = false
    )

    data object DietaryRestrictions : OnboardingScreen(
        route = "dietary_restrictions",
        stepNumber = 1,
        title = "Dietary Restrictions",
        description = "We'll NEVER recommend meals that violate these",
        isSkippable = true
    )

    data object Allergies : OnboardingScreen(
        route = "allergies",
        stepNumber = 2,
        title = "Food Allergies",
        description = "Critical for your safety - these will be completely excluded",
        isSkippable = true
    )

    data object AvoidIngredients : OnboardingScreen(
        route = "avoid_ingredients",
        stepNumber = 3,
        title = "Ingredients to Avoid",
        description = "Ingredients you absolutely refuse to eat",
        isSkippable = true
    )

    data object FavoriteCuisines : OnboardingScreen(
        route = "favorite_cuisines",
        stepNumber = 4,
        title = "Favorite Cuisines",
        description = "What do you usually enjoy?",
        isSkippable = true
    )

    data object SpiceTolerance : OnboardingScreen(
        route = "spice_tolerance",
        stepNumber = 5,
        title = "Spice Preference",
        description = "How spicy do you like your food?",
        isSkippable = true
    )

    data object Summary : OnboardingScreen(
        route = "summary",
        stepNumber = 6,
        title = "Review & Finish",
        description = "Review your preferences and start using Veato!",
        isSkippable = false
    )

    companion object {
        val ALL_SCREENS = listOf(
            Welcome,
            DietaryRestrictions,
            Allergies,
            AvoidIngredients,
            FavoriteCuisines,
            SpiceTolerance,
            Summary
        )

        val TOTAL_STEPS = ALL_SCREENS.size

        fun fromRoute(route: String): OnboardingScreen? {
            return ALL_SCREENS.find { it.route == route }
        }
    }
}
