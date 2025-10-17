package com.example.veato.ui.profile

import com.example.veato.data.model.UserProfile

data class ProfileState(
    val userProfile: UserProfile? = null,
    val isBusy: Boolean = true,
    val tab: Int = 0, // 0 for preferences, 1 for history
    val isEditing: Boolean = false,
    val saveError: String? = null
)