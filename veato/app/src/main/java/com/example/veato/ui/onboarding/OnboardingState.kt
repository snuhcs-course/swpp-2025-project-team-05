package com.example.veato.ui.onboarding

import com.example.veato.data.model.UserProfile

/**
 * State for the onboarding flow
 */
data class OnboardingState(
    val currentScreen: OnboardingScreen = OnboardingScreen.Welcome,
    val profileDraft: UserProfile = UserProfile.createNew("temp_user"), // Will be replaced with actual user ID
    val validationErrors: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val isComplete: Boolean = false
) {
    /**
     * Get current step number (1-based for display)
     */
    val currentStepNumber: Int
        get() = currentScreen.stepNumber + 1

    /**
     * Total number of steps
     */
    val totalSteps: Int
        get() = OnboardingScreen.TOTAL_STEPS

    /**
     * Check if can navigate to next screen
     */
    fun canNavigateNext(): Boolean {
        return !isSaving && currentScreen != OnboardingScreen.Summary
    }

    /**
     * Check if can navigate to previous screen
     */
    fun canNavigatePrevious(): Boolean {
        return !isSaving && currentScreen != OnboardingScreen.Welcome
    }

    /**
     * Get progress percentage (0.0 to 1.0)
     */
    fun getProgressPercentage(): Float {
        return currentStepNumber.toFloat() / totalSteps.toFloat()
    }

    /**
     * Check if current screen is a hard constraint screen
     */
    fun isHardConstraintScreen(): Boolean {
        return currentScreen in listOf(
            OnboardingScreen.DietaryRestrictions,
            OnboardingScreen.Allergies,
            OnboardingScreen.AvoidIngredients,
            OnboardingScreen.BudgetCap
        )
    }

    /**
     * Check if current screen is a soft preference screen
     */
    fun isSoftPreferenceScreen(): Boolean {
        return currentScreen in listOf(
            OnboardingScreen.FavoriteCuisines,
            OnboardingScreen.SpiceTolerance,
            OnboardingScreen.HeavinessPreference,
            OnboardingScreen.BudgetPreference,
            OnboardingScreen.AdvancedPreferences
        )
    }
}
