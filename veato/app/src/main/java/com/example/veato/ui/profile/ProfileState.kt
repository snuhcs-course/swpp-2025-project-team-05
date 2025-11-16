package com.example.veato.ui.profile

import android.net.Uri
import com.example.veato.data.model.UserProfile

data class ProfileState(
    val userProfile: UserProfile? = null,
    val isBusy: Boolean = true,
    val isEditing: Boolean = false,
    val saveError: String? = null,
    val editedFullName: String = "",
    val editedUserName: String = "",
    val selectedImageUri: Uri? = null,
    val isUploadingImage: Boolean = false,
    val showImagePicker: Boolean = false
)