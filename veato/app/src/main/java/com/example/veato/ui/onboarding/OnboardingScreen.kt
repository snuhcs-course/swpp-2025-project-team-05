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

    data object BudgetCap : OnboardingScreen(
        route = "budget_cap",
        stepNumber = 4,
        title = "Budget Cap",
        description = "Maximum amount per meal - we won't suggest anything above this",
        isSkippable = true
    )

    data object FavoriteCuisines : OnboardingScreen(
        route = "favorite_cuisines",
        stepNumber = 5,
        title = "Favorite Cuisines",
        description = "What do you usually enjoy?",
        isSkippable = true
    )

    data object SpiceTolerance : OnboardingScreen(
        route = "spice_tolerance",
        stepNumber = 6,
        title = "Spice Preference",
        description = "How spicy do you like your food?",
        isSkippable = true
    )

    data object HeavinessPreference : OnboardingScreen(
        route = "heaviness_preference",
        stepNumber = 7,
        title = "Meal Heaviness",
        description = "Do you prefer light or filling meals?",
        isSkippable = true
    )

    data object BudgetPreference : OnboardingScreen(
        route = "budget_preference",
        stepNumber = 8,
        title = "Typical Budget",
        description = "What's your usual budget range?",
        isSkippable = true
    )

    data object AdvancedPreferences : OnboardingScreen(
        route = "advanced_preferences",
        stepNumber = 9,
        title = "Advanced Preferences",
        description = "Optional: Meal type, portion size, and more",
        isSkippable = true
    )

    data object Summary : OnboardingScreen(
        route = "summary",
        stepNumber = 10,
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
            BudgetCap,
            FavoriteCuisines,
            SpiceTolerance,
            HeavinessPreference,
            BudgetPreference,
            AdvancedPreferences,
            Summary
        )

        val TOTAL_STEPS = ALL_SCREENS.size

        fun fromRoute(route: String): OnboardingScreen? {
            return ALL_SCREENS.find { it.route == route }
        }
    }
}
