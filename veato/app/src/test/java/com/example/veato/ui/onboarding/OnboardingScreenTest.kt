package com.example.veato.ui.onboarding

import org.junit.Assert.*
import org.junit.Test

class OnboardingScreenTest {

    @Test
    fun allScreens_haveUniqueRoutesAndStepNumbers() {
        val routes = OnboardingScreen.ALL_SCREENS.map { it.route }
        val steps = OnboardingScreen.ALL_SCREENS.map { it.stepNumber }

        assertEquals(routes.size, routes.toSet().size)   // No duplicates
        assertEquals(steps.size, steps.toSet().size)     // No duplicates
    }

    @Test
    fun totalSteps_matchesAllScreensSize() {
        assertEquals(
            OnboardingScreen.ALL_SCREENS.size,
            OnboardingScreen.TOTAL_STEPS
        )
    }

    @Test
    fun fromRoute_validRoutes_returnCorrectScreen() {
        for (screen in OnboardingScreen.ALL_SCREENS) {
            val route: String = screen.route    // route is non-null

            val resolved = OnboardingScreen.fromRoute(route)
            assertEquals(screen, resolved)
        }
    }

    @Test
    fun fromRoute_invalidRoute_returnsNull() {
        val invalidRoute = "not_a_real_route"

        val result = OnboardingScreen.fromRoute(invalidRoute)

        assertNull(result)
    }

    @Test
    fun screens_haveExpectedTitles() {
        assertEquals("Welcome to Veato!", OnboardingScreen.Welcome.title)
        assertEquals("Dietary Restrictions", OnboardingScreen.DietaryRestrictions.title)
        assertEquals("Food Allergies", OnboardingScreen.Allergies.title)
        assertEquals("Ingredients to Avoid", OnboardingScreen.AvoidIngredients.title)
        assertEquals("Favorite Cuisines", OnboardingScreen.FavoriteCuisines.title)
        assertEquals("Spice Preference", OnboardingScreen.SpiceTolerance.title)
        assertEquals("Review & Finish", OnboardingScreen.Summary.title)
    }

    @Test
    fun skippableScreens_areCorrect() {
        val skippableScreens = listOf(
            OnboardingScreen.DietaryRestrictions,
            OnboardingScreen.Allergies,
            OnboardingScreen.AvoidIngredients,
            OnboardingScreen.FavoriteCuisines,
            OnboardingScreen.SpiceTolerance
        )

        for (screen in OnboardingScreen.ALL_SCREENS) {
            assertEquals(
                screen in skippableScreens,
                screen.isSkippable
            )
        }
    }
}
