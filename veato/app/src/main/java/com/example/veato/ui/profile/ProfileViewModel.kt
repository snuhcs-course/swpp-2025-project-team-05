package com.example.veato.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veato.data.model.Allergen
import com.example.veato.data.model.CuisineType
import com.example.veato.data.model.DietaryType
import com.example.veato.data.model.SpiceLevel
import com.example.veato.data.repository.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: UserProfileRepository,
    private val userId: String
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, saveError = null) }

            try {
                val profile = repository.getProfile(userId)
                _state.update { it.copy(userProfile = profile, isBusy = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isBusy = false, saveError = e.localizedMessage) }
            }
        }
    }

    fun toggleEditing() {
        _state.update { it.copy(isEditing = !it.isEditing) }
    }

    fun changeTab(newTab: Int) {
        _state.update { it.copy(tab = newTab) }
    }


    fun updateDietaryRestrictions(newList: List<DietaryType>) {
        _state.update { current ->
            val updatedProfile = current.userProfile?.copy(
                hardConstraints = current.userProfile.hardConstraints.copy(
                    dietaryRestrictions = newList
                )
            )
            current.copy(userProfile = updatedProfile)
        }
    }

    fun updateAllergies(newList: List<Allergen>) {
        _state.update { current ->
            val updatedProfile = current.userProfile?.copy(
                hardConstraints = current.userProfile.hardConstraints.copy(
                    allergies = newList
                )
            )
            current.copy(userProfile = updatedProfile)
        }
    }

    fun updateFavoriteCuisines(newList: List<CuisineType>) {
        _state.update { current ->
            val updatedProfile = current.userProfile?.copy(
                softPreferences = current.userProfile.softPreferences.copy(
                    favoriteCuisines = newList
                )
            )
            current.copy(userProfile = updatedProfile)
        }
    }

    fun updateSpiceTolerance(newLevel: SpiceLevel) {
        _state.update { current ->
            val updatedProfile = current.userProfile?.copy(
                softPreferences = current.userProfile.softPreferences.copy(
                    spiceTolerance = newLevel
                )
            )
            current.copy(userProfile = updatedProfile)
        }
    }


    fun updateProfile() {
        viewModelScope.launch {
            val profileToUpdate = _state.value.userProfile ?: return@launch

            _state.update { it.copy(isBusy = true, saveError = null) }

            val result = repository.updateProfile(profileToUpdate)

            if (result.isSuccess) {
                loadProfile()
                _state.update { it.copy(isEditing = false) }
            } else {
                _state.update {
                    it.copy(isBusy = false, saveError = result.exceptionOrNull()?.message)
                }
            }
        }
    }
}