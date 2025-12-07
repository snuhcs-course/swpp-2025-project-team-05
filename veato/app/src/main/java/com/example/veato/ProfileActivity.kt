package com.example.veato

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.veato.data.di.DefaultRepositoryFactory
import com.example.veato.data.local.ProfileDataStoreImpl
import com.example.veato.data.model.Allergen
import com.example.veato.data.model.DietaryType
import com.example.veato.data.model.UserProfile
import com.example.veato.data.model.getIconResource
import com.example.veato.data.remote.ProfileApiDataSource
import com.example.veato.data.repository.UserProfileRepositoryImpl
import com.example.veato.ui.components.IngredientsChipInput
import com.example.veato.ui.components.MultiSelectChipGroup
import com.example.veato.ui.main.MainActivity
import com.example.veato.ui.profile.ProfileViewModel
import com.example.veato.ui.profile.ProfileViewModelFactory
import com.example.veato.ui.theme.VeatoTheme
import com.example.veato.ui.components.VeatoBottomNavigationBar
import com.example.veato.ui.components.NavigationScreen
import com.example.veato.ui.components.EXTRA_FROM_TAB_INDEX

import com.google.firebase.auth.FirebaseAuth


class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply transition animation based on tab direction
        applyTabTransition()

        setContent {
            VeatoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ProfileScreen()
                }
            }
        }
    }

    private fun applyTabTransition() {
        val fromIndex = intent.getIntExtra(EXTRA_FROM_TAB_INDEX, -1)
        val toIndex = NavigationScreen.PROFILE.index

        if (fromIndex != -1) {
            if (toIndex > fromIndex) {
                // Moving right: slide content from right to left
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            } else if (toIndex < fromIndex) {
                // Moving left: slide content from left to right
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "demo_user"

    // Use Factory Method pattern to create repository
    val repositoryFactory = remember { DefaultRepositoryFactory() }
    val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(
            repository = repositoryFactory.createUserProfileRepository(context),
            userId = userId
        )
    )
    val state by viewModel.state.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar on save success
    LaunchedEffect(state.saveSuccess) {
        state.saveSuccess?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    // Image picker launchers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.selectImage(it) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // Image saved to the URI provided
            Toast.makeText(context, "Photo captured", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            if (state.isEditing) {
                TopAppBar(
                    title = { Text("Edit Profile") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.toggleEditing() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.updateProfile() }) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Profile") }
                )
            }
        },
        bottomBar = {
            VeatoBottomNavigationBar(currentScreen = NavigationScreen.PROFILE)
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        when {
            state.isBusy -> LoadingCircle()
            state.userProfile == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            "Unable to load profile",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Check your internet connection and try again",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadProfile() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(16.dp))

                    // Profile Picture
                    ProfilePictureSection(
                        profilePictureUrl = state.selectedImageUri?.toString()
                            ?: state.userProfile?.profilePictureUrl ?: "",
                        isEditing = state.isEditing,
                        onCameraClick = {
                            // Show dialog to choose camera or gallery
                            galleryLauncher.launch("image/*")
                        }
                    )

                    Spacer(Modifier.height(16.dp))

                    // Full Name and Username
                    if (state.isEditing) {
                        OutlinedTextField(
                            value = state.editedFullName,
                            onValueChange = { viewModel.updateEditedFullName(it) },
                            label = { Text("Full Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.editedUserName,
                            onValueChange = { viewModel.updateEditedUserName(it) },
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            prefix = { Text("@") }
                        )
                    } else {
                        Text(
                            text = state.userProfile?.fullName ?: "Full Name",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "@${state.userProfile?.userName ?: "username"}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // Edit Profile Button (when not editing)
                    if (!state.isEditing) {
                        Button(
                            onClick = { viewModel.toggleEditing() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("Edit Profile")
                        }
                        Spacer(Modifier.height(24.dp))
                    }

                    // Error message
                    state.saveError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    // Hard Constraints Section
                    Text(
                        "Dietary Preferences",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))

                    DietaryRestrictionsSection(
                        enabled = state.isEditing,
                        selectedItems = state.userProfile?.hardConstraints?.dietaryRestrictions
                            ?: emptyList(),
                        onUpdate = viewModel::updateDietaryRestrictions
                    )

                    Spacer(Modifier.height(16.dp))

                    AllergiesSection(
                        enabled = state.isEditing,
                        selectedItems = state.userProfile?.hardConstraints?.allergies
                            ?: emptyList(),
                        onUpdate = viewModel::updateAllergies
                    )

                    Spacer(Modifier.height(16.dp))

                    IngredientsToAvoidSection(
                        enabled = state.isEditing,
                        ingredients = state.userProfile?.hardConstraints?.avoidIngredients
                            ?: emptyList(),
                        onUpdate = viewModel::updateAvoidIngredients
                    )

                    Spacer(Modifier.height(24.dp))

                    // Logout Button
                    Button(
                        onClick = {
                            FirebaseAuth.getInstance().signOut()
                            val intent = Intent(context, com.example.veato.ui.auth.LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            context.startActivity(intent)
                            (context as? ProfileActivity)?.finish()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Log Out")
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfilePictureSection(
    profilePictureUrl: String,
    isEditing: Boolean,
    onCameraClick: () -> Unit
) {
    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // Profile Picture
        if (profilePictureUrl.isNotEmpty()) {
            Image(
                painter = rememberAsyncImagePainter(profilePictureUrl),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .clickable(enabled = isEditing) { onCameraClick() },
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
                    .clickable(enabled = isEditing) { onCameraClick() }
            )
        }

        // Camera icon overlay when editing
        if (isEditing) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onCameraClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Change picture",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DietaryRestrictionsSection(
    enabled: Boolean,
    selectedItems: List<DietaryType>,
    onUpdate: (List<DietaryType>) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Dietary Restrictions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(12.dp))
        MultiSelectChipGroup(
            items = DietaryType.entries,
            selectedItems = selectedItems,
            onSelectionChange = onUpdate,
            itemLabel = { it.displayName },
            itemIcon = { it.getIconResource() },
            itemsPerRow = 2,
            enabled = enabled
        )
    }
}

@Composable
private fun AllergiesSection(
    enabled: Boolean,
    selectedItems: List<Allergen>,
    onUpdate: (List<Allergen>) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Food Allergies",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(12.dp))
        MultiSelectChipGroup(
            items = Allergen.entries,
            selectedItems = selectedItems,
            onSelectionChange = onUpdate,
            itemLabel = { it.displayName },
            itemIcon = { it.getIconResource() },
            itemsPerRow = 2,
            enabled = enabled
        )
    }
}

@Composable
private fun IngredientsToAvoidSection(
    enabled: Boolean,
    ingredients: List<String>,
    onUpdate: (List<String>) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Ingredients to Avoid",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(12.dp))
        IngredientsChipInput(
            ingredients = ingredients,
            onIngredientsChange = onUpdate,
            enabled = enabled
        )
    }
}

@Composable
private fun LoadingCircle() {
    Box(Modifier.fillMaxSize()) {
        CircularProgressIndicator(Modifier.align(Alignment.Center))
    }
}

