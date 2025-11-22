package com.example.veato.ui.onboarding

import com.example.veato.data.model.UserProfile
import org.junit.Assert.*
import org.junit.Test

class OnboardingStateTest {

    @Test
    fun initialState_hasCorrectDefaults() {
        val state = OnboardingState()

        assertEquals(OnboardingScreen.Welcome, state.currentScreen)
        assertFalse(state.isSaving)
        assertFalse(state.isComplete)
        assertNull(state.saveError)
        assertTrue(state.validationErrors.isEmpty())
        assertEquals("temp_user", state.profileDraft.userId)
    }

    @Test
    fun currentStepNumber_matchesScreenStepPlusOne() {
        val state = OnboardingState(currentScreen = OnboardingScreen.Allergies)
        assertEquals(OnboardingScreen.Allergies.stepNumber + 1, state.currentStepNumber)
    }

    @Test
    fun totalSteps_matchesOnboardingScreenCount() {
        val state = OnboardingState()
        assertEquals(OnboardingScreen.TOTAL_STEPS, state.totalSteps)
    }

    @Test
    fun canNavigateNext_returnsFalse_onSummaryScreen() {
        val state = OnboardingState(currentScreen = OnboardingScreen.Summary)
        assertFalse(state.canNavigateNext())
    }

    @Test
    fun canNavigateNext_returnsFalse_whenSaving() {
        val state = OnboardingState(isSaving = true)
        assertFalse(state.canNavigateNext())
    }

    @Test
    fun canNavigateNext_returnsTrue_onNormalScreen() {
        val state = OnboardingState(currentScreen = OnboardingScreen.Allergies)
        assertTrue(state.canNavigateNext())
    }

    @Test
    fun canNavigatePrevious_returnsFalse_onWelcomeScreen() {
        val state = OnboardingState(currentScreen = OnboardingScreen.Welcome)
        assertFalse(state.canNavigatePrevious())
    }

    @Test
    fun canNavigatePrevious_returnsFalse_whenSaving() {
        val state = OnboardingState(isSaving = true)
        assertFalse(state.canNavigatePrevious())
    }

    @Test
    fun canNavigatePrevious_returnsTrue_onMiddleScreen() {
        val state = OnboardingState(currentScreen = OnboardingScreen.AvoidIngredients)
        assertTrue(state.canNavigatePrevious())
    }

    @Test
    fun getProgressPercentage_isCorrectForScreen() {
        val state = OnboardingState(currentScreen = OnboardingScreen.FavoriteCuisines)
        val expected = state.currentStepNumber.toFloat() / state.totalSteps.toFloat()

        assertEquals(expected, state.getProgressPercentage(), 0.0001f)
    }

    @Test
    fun isHardConstraintScreen_returnsTrue_forHardScreens() {
        val hardScreens = listOf(
            OnboardingScreen.DietaryRestrictions,
            OnboardingScreen.Allergies,
            OnboardingScreen.AvoidIngredients
        )

        for (screen in hardScreens) {
            val state = OnboardingState(currentScreen = screen)
            assertTrue("Expected hard screen: ${screen.route}", state.isHardConstraintScreen())
        }
    }

    @Test
    fun isHardConstraintScreen_returnsFalse_forNonHardScreens() {
        val nonHardScreens = listOf(
            OnboardingScreen.Welcome,
            OnboardingScreen.FavoriteCuisines,
            OnboardingScreen.SpiceTolerance,
            OnboardingScreen.Summary
        )

        for (screen in nonHardScreens) {
            val state = OnboardingState(currentScreen = screen)
            assertFalse("Not a hard screen: ${screen.route}", state.isHardConstraintScreen())
        }
    }

    @Test
    fun isSoftPreferenceScreen_returnsTrue_forSoftScreens() {
        val softScreens = listOf(
            OnboardingScreen.FavoriteCuisines,
            OnboardingScreen.SpiceTolerance
        )

        for (screen in softScreens) {
            val state = OnboardingState(currentScreen = screen)
            assertTrue("Expected soft screen: ${screen.route}", state.isSoftPreferenceScreen())
        }
    }

    @Test
    fun isSoftPreferenceScreen_returnsFalse_forNonSoftScreens() {
        val nonSoftScreens = listOf(
            OnboardingScreen.Welcome,
            OnboardingScreen.DietaryRestrictions,
            OnboardingScreen.Allergies,
            OnboardingScreen.AvoidIngredients,
            OnboardingScreen.Summary
        )

        for (screen in nonSoftScreens) {
            val state = OnboardingState(currentScreen = screen)
            assertFalse("Not a soft screen: ${screen.route}", state.isSoftPreferenceScreen())
        }
    }

    @Test
    fun state_withSaveError_holdsErrorCorrectly() {
        val msg = "Network down"
        val state = OnboardingState(saveError = msg)

        assertEquals(msg, state.saveError)
    }

    @Test
    fun state_markedComplete_isCorrect() {
        val state = OnboardingState(isComplete = true)
        assertTrue(state.isComplete)
    }

    @Test
    fun state_markedSaving_isCorrect() {
        val state = OnboardingState(isSaving = true)
        assertTrue(state.isSaving)
    }
}
