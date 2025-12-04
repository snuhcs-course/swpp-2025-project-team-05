package com.example.veato

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.veato.data.local.ProfileDataStoreImpl
import com.example.veato.data.model.CuisineType
import com.example.veato.data.model.SpiceLevel
import com.example.veato.data.model.getIconResource
import com.example.veato.data.remote.ProfileApiDataSource
import com.example.veato.data.repository.UserProfileRepositoryImpl
import com.example.veato.ui.components.MultiSelectChipGroup
import com.example.veato.ui.profile.ProfileViewModel
import com.example.veato.ui.profile.ProfileViewModelFactory
import com.example.veato.ui.theme.VeatoTheme
import com.example.veato.ui.components.VeatoBottomNavigationBar
import com.example.veato.ui.components.NavigationScreen
import com.example.veato.ui.components.EXTRA_FROM_TAB_INDEX

import com.google.firebase.auth.FirebaseAuth

class MyPreferencesActivity : ComponentActivity() {
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
                    MyPreferencesScreen()
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MyPreferencesScreen() {
        val context = LocalContext.current
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "demo_user"

        val viewModel: ProfileViewModel = viewModel(
            factory = ProfileViewModelFactory(
                repository = UserProfileRepositoryImpl(
                    ProfileDataStoreImpl(context),
                    ProfileApiDataSource()
                ),
                userId = userId
            )
        )
        val state by viewModel.state.collectAsState()

        val snackbarHostState = remember { SnackbarHostState() }

        // Show snackbar on save error
        LaunchedEffect(state.saveError) {
            state.saveError?.let { error ->
                snackbarHostState.showSnackbar(
                    message = error,
                    actionLabel = "Dismiss",
                    duration = SnackbarDuration.Long
                )
            }
        }

        // Show snackbar on save success
        LaunchedEffect(state.saveSuccess) {
            state.saveSuccess?.let { message ->
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
        }

        Scaffold(
            topBar = {
                if (state.isEditing) {
                    TopAppBar(
                        title = { Text("Edit Preferences") },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.toggleEditing() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel"
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                state.userProfile?.let { profile ->
                                    viewModel.updateProfileData(profile)
                                }
                            }) {
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
                        title = { Text("My Preferences") }
                    )
                }
            },
            bottomBar = {
                VeatoBottomNavigationBar(currentScreen = NavigationScreen.PREFERENCES)
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { paddingValues ->
            when {
                state.isBusy -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
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
                            .padding(24.dp)
                    ) {
                        Text(
                            "What I want to eat right now",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "These settings only affect ranking. Your restrictions still apply.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(24.dp))

                        // Edit Preferences Button (when not editing)
                        if (!state.isEditing) {
                            Button(
                                onClick = { viewModel.toggleEditing() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text("Edit Preferences")
                            }
                            Spacer(Modifier.height(24.dp))
                        }

                        // Favorite Cuisines
                        Text(
                            "Favorite Cuisines",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        MultiSelectChipGroup(
                            items = CuisineType.entries,
                            selectedItems = state.userProfile?.softPreferences?.favoriteCuisines ?: emptyList(),
                            onSelectionChange = { selected ->
                                viewModel.updateFavoriteCuisines(selected)
                            },
                            itemLabel = { "${it.koreanName} ${it.displayName}" },
                            itemIcon = { it.getIconResource() },
                            enabled = state.isEditing
                        )

                        Spacer(Modifier.height(24.dp))

                        // Your Spice Level (using slider)
                        SpiceSlider(
                            selectedLevel = state.userProfile?.softPreferences?.spiceTolerance,
                            onLevelSelected = { level ->
                                viewModel.updateSpiceTolerance(level)
                            },
                            enabled = state.isEditing
                        )

                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    @Composable
    private fun SpiceSlider(
        selectedLevel: SpiceLevel?,
        onLevelSelected: (SpiceLevel) -> Unit,
        enabled: Boolean = true,
        modifier: Modifier = Modifier
    ) {
        // Map SpiceLevel to slider position (0-4) and vice versa
        val spiceLevels = listOf(
            SpiceLevel.NONE,    // position 0
            SpiceLevel.LOW,     // position 1
            SpiceLevel.MEDIUM,  // position 2
            SpiceLevel.HIGH,    // position 3
            SpiceLevel.EXTRA    // position 4
        )

        val currentPosition = selectedLevel?.let { level ->
            spiceLevels.indexOf(level).toFloat()
        } ?: 2f  // Default to MEDIUM if null

        val labels = listOf("No", "Low", "Medium", "High", "Extra")

        Column(modifier = modifier.fillMaxWidth()) {
            // Label: "Your Spice Level"
            Text(
                "Your Spice Level",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(8.dp))

            // Live value display
            Text(
                selectedLevel?.displayName ?: "Medium Spice Preferred",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(16.dp))

            // Slider with labels
            Column {
                Slider(
                    value = currentPosition,
                    onValueChange = { value ->
                        val position = value.toInt().coerceIn(0, 4)
                        onLevelSelected(spiceLevels[position])
                    },
                    valueRange = 0f..4f,
                    steps = 3,  // 5 total positions (0, 1, 2, 3, 4) = 3 steps between
                    enabled = enabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Spice level, currently ${selectedLevel?.displayName ?: "Medium"}"
                        }
                )

                // Snap point labels below slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    labels.forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    private fun applyTabTransition() {
        val fromIndex = intent.getIntExtra(EXTRA_FROM_TAB_INDEX, -1)
        val toIndex = NavigationScreen.PREFERENCES.index

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
