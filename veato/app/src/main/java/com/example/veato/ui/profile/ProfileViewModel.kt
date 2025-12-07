package com.example.veato.ui.profile

import android.net.Uri
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

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
            loadProfileSuspend()
        }
    }

    private suspend fun loadProfileSuspend() {
        _state.update { it.copy(isBusy = true, saveError = null, saveSuccess = null) }

        try {
            // Try to load from remote with timeout
            val profile = try {
                withTimeout(5000L) { // 5 second timeout
                    repository.getProfile(userId)
                }
            } catch (e: Exception) {
                null // If timeout or error, fall back to local
            }

            if (profile != null) {
                _state.update {
                    it.copy(
                        userProfile = profile,
                        isBusy = false,
                        editedFullName = profile.fullName,
                        editedUserName = profile.userName
                    )
                }
            } else {
                // If remote fetch fails, try local data
                val localProfile = repository.getProfileFlow(userId).first()
                _state.update {
                    it.copy(
                        userProfile = localProfile,
                        isBusy = false,
                        editedFullName = localProfile?.fullName ?: "",
                        editedUserName = localProfile?.userName ?: "",
                        saveError = if (localProfile == null) "Failed to load profile" else null
                    )
                }
            }
        } catch (e: Exception) {
            _state.update { it.copy(isBusy = false, saveError = e.localizedMessage) }
        }
    }

    fun toggleEditing() {
        val currentState = _state.value
        val newEditingState = !currentState.isEditing

        // When starting to edit, initialize the edited fields from current profile
        if (newEditingState) {
            _state.update {
                it.copy(
                    isEditing = true,
                    editedFullName = it.userProfile?.fullName ?: "",
                    editedUserName = it.userProfile?.userName ?: "",
                    selectedImageUri = null
                )
            }
        } else {
            // When canceling edit, reload profile to revert UI to saved state
            _state.update {
                it.copy(
                    isEditing = false,
                    selectedImageUri = null,
                    saveError = null,
                    saveSuccess = null
                )
            }
            // Reload profile from backend to discard in-memory edits
            loadProfile()
        }
    }

    fun updateEditedFullName(newName: String) {
        _state.update { it.copy(editedFullName = newName) }
    }

    fun updateEditedUserName(newUserName: String) {
        _state.update { it.copy(editedUserName = newUserName) }
    }

    fun selectImage(uri: Uri?) {
        _state.update { it.copy(selectedImageUri = uri) }
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

    fun updateAvoidIngredients(newList: List<String>) {
        _state.update { current ->
            val updatedProfile = current.userProfile?.copy(
                hardConstraints = current.userProfile.hardConstraints.copy(
                    avoidIngredients = newList
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


    suspend fun uploadImage(imageUri: Uri): Result<String> {
        return try {
            _state.update { it.copy(isUploadingImage = true) }
            val downloadUrl = repository.uploadProfileImage(userId, imageUri)
            _state.update { it.copy(isUploadingImage = false) }
            Result.success(downloadUrl)
        } catch (e: Exception) {
            _state.update { it.copy(isUploadingImage = false) }
            Result.failure(e)
        }
    }

    fun updateProfile() {
        viewModelScope.launch {
            val currentProfile = _state.value.userProfile ?: return@launch
            val currentState = _state.value

            // Validate input
            if (currentState.editedFullName.isBlank() || currentState.editedUserName.isBlank()) {
                _state.update { it.copy(saveError = "Name and username cannot be empty") }
                return@launch
            }

            _state.update { it.copy(isBusy = true, saveError = null, saveSuccess = null) }

            try {
                // Upload image if selected
                var profilePictureUrl = currentProfile.profilePictureUrl
                currentState.selectedImageUri?.let { uri ->
                    val uploadResult = uploadImage(uri)
                    if (uploadResult.isSuccess) {
                        profilePictureUrl = uploadResult.getOrNull() ?: profilePictureUrl
                    } else {
                        _state.update {
                            it.copy(
                                isBusy = false,
                                saveError = "Failed to upload image: ${uploadResult.exceptionOrNull()?.message}"
                            )
                        }
                        return@launch
                    }
                }

                // Update profile with edited fields
                val profileToUpdate = currentProfile.copy(
                    fullName = currentState.editedFullName.trim(),
                    userName = currentState.editedUserName.trim(),
                    profilePictureUrl = profilePictureUrl
                )

                val result = repository.updateProfile(profileToUpdate)

                if (result.isSuccess) {
                    // Reload profile and wait for it to complete
                    loadProfileSuspend()
                    // Only update editing state after profile is loaded
                    _state.update { it.copy(isEditing = false, selectedImageUri = null, saveSuccess = "Profile updated") }
                } else {
                    _state.update {
                        it.copy(isBusy = false, saveError = result.exceptionOrNull()?.message)
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isBusy = false, saveError = e.localizedMessage)
                }
            }
        }
    }

    fun updateProfileData(updatedProfile: com.example.veato.data.model.UserProfile) {
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, saveError = null, saveSuccess = null) }
            try {
                val result = repository.updateProfile(updatedProfile)
                if (result.isSuccess) {
                    // Update local state immediately instead of reloading from server
                    _state.update {
                        it.copy(
                            userProfile = updatedProfile,
                            isBusy = false,
                            isEditing = false,
                            saveSuccess = "Preferences updated"
                        )
                    }
                } else {
                    _state.update {
                        it.copy(isBusy = false, saveError = result.exceptionOrNull()?.message)
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isBusy = false, saveError = e.localizedMessage)
                }
            }
        }
    }
}