package com.example.veato.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veato.data.model.*
import com.example.veato.data.repository.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val repository: UserProfileRepository,
    private val userId: String
) : ViewModel() {

    private val _state = MutableStateFlow(
        OnboardingState(profileDraft = UserProfile.createNew(userId))
    )
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    // Update hard constraints
    fun updateDietaryRestrictions(restrictions: List<DietaryType>) {
        _state.update { currentState ->
            val updatedConstraints = currentState.profileDraft.hardConstraints.copy(
                dietaryRestrictions = restrictions
            )
            currentState.copy(
                profileDraft = currentState.profileDraft.copy(hardConstraints = updatedConstraints)
            )
        }
    }

    fun updateAllergies(allergies: List<Allergen>) {
        _state.update { currentState ->
            val updatedConstraints = currentState.profileDraft.hardConstraints.copy(
                allergies = allergies
            )
            currentState.copy(
                profileDraft = currentState.profileDraft.copy(hardConstraints = updatedConstraints)
            )
        }
    }

    fun updateAvoidIngredients(ingredients: List<String>) {
        _state.update { currentState ->
            val updatedConstraints = currentState.profileDraft.hardConstraints.copy(
                avoidIngredients = ingredients
            )
            currentState.copy(
                profileDraft = currentState.profileDraft.copy(hardConstraints = updatedConstraints)
            )
        }
    }

    // Update soft preferences
    fun updateFavoriteCuisines(cuisines: List<CuisineType>) {
        _state.update { currentState ->
            val updatedPreferences = currentState.profileDraft.softPreferences.copy(
                favoriteCuisines = cuisines
            )
            currentState.copy(
                profileDraft = currentState.profileDraft.copy(softPreferences = updatedPreferences)
            )
        }
    }

    fun updateSpiceTolerance(level: SpiceLevel) {
        _state.update { currentState ->
            val updatedPreferences = currentState.profileDraft.softPreferences.copy(
                spiceTolerance = level
            )
            currentState.copy(
                profileDraft = currentState.profileDraft.copy(softPreferences = updatedPreferences)
            )
        }
    }

    // Navigation
    fun nextScreen() {
        _state.update { currentState ->
            if (currentState.canNavigateNext()) {
                val nextScreen = getNextScreen(currentState.currentScreen)
                if (nextScreen != currentState.currentScreen) {
                    currentState.copy(
                        currentScreen = nextScreen,
                        validationErrors = emptyList()
                    )
                } else {
                    currentState
                }
            } else {
                currentState
            }
        }
    }

    fun previousScreen() {
        _state.update { currentState ->
            if (currentState.canNavigatePrevious()) {
                val prevScreen = getPreviousScreen(currentState.currentScreen)
                currentState.copy(
                    currentScreen = prevScreen,
                    validationErrors = emptyList()
                )
            } else {
                currentState
            }
        }
    }

    fun skipToNextSection() {
        // Skip to next major section or just go to next screen
        nextScreen()
    }

    fun navigateToScreen(screen: OnboardingScreen) {
        _state.update { it.copy(currentScreen = screen) }
    }

    // Save profile
    fun saveProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, saveError = null) }

            val profileToSave = _state.value.profileDraft.copy(
                isOnboardingComplete = true
            )

            val result = repository.saveProfile(profileToSave)

            _state.update { currentState ->
                if (result.isSuccess) {
                    currentState.copy(
                        isSaving = false,
                        isComplete = true,
                        saveError = null
                    )
                } else {
                    currentState.copy(
                        isSaving = false,
                        saveError = result.exceptionOrNull()?.message ?: "Failed to save profile"
                    )
                }
            }
        }
    }

    // Validation
    fun validateCurrentScreen(): Boolean {
        val currentState = _state.value
        val errors = mutableListOf<String>()

        // No specific validation needed for current screens

        _state.update { it.copy(validationErrors = errors) }
        return errors.isEmpty()
    }

    // Helper methods
    private fun getNextScreen(current: OnboardingScreen): OnboardingScreen {
        // Use stepNumber for reliable indexing instead of indexOf
        val currentStep = current.stepNumber
        return if (currentStep < OnboardingScreen.ALL_SCREENS.size - 1) {
            OnboardingScreen.ALL_SCREENS[currentStep + 1]
        } else {
            current
        }
    }

    private fun getPreviousScreen(current: OnboardingScreen): OnboardingScreen {
        // Use stepNumber for reliable indexing instead of indexOf
        val currentStep = current.stepNumber
        return if (currentStep > 0) {
            OnboardingScreen.ALL_SCREENS[currentStep - 1]
        } else {
            current
        }
    }
}
