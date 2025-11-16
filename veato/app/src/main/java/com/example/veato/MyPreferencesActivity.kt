package com.example.veato

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.veato.data.local.ProfileDataStoreImpl
import com.example.veato.data.model.CuisineType
import com.example.veato.data.model.SpiceLevel
import com.example.veato.data.remote.ProfileApiDataSource
import com.example.veato.data.repository.UserProfileRepositoryImpl
import com.example.veato.ui.components.MultiSelectChipGroup
import com.example.veato.ui.profile.ProfileViewModel
import com.example.veato.ui.profile.ProfileViewModelFactory
import com.example.veato.ui.theme.VeatoTheme
import com.google.firebase.auth.FirebaseAuth

class MyPreferencesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "My Preferences",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                BottomNavigationBar(currentScreen = "Preferences")
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
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No profile found.")
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
                            "Soft Preferences",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(24.dp))

                        // Favorite Cuisines
                        Text(
                            "Favorite Cuisines",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        MultiSelectChipGroup(
                            items = CuisineType.values().toList(),
                            selectedItems = state.userProfile?.softPreferences?.favoriteCuisines ?: emptyList(),
                            onSelectionChange = { selected ->
                                val updatedProfile = state.userProfile?.copy(
                                    softPreferences = state.userProfile!!.softPreferences.copy(
                                        favoriteCuisines = selected
                                    )
                                )
                                updatedProfile?.let {
                                    viewModel.updateProfileData(it)
                                }
                            },
                            itemLabel = { it.name },
                            enabled = true
                        )

                        Spacer(Modifier.height(24.dp))

                        // Spice Tolerance
                        Text(
                            "Spice Tolerance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        SpiceLevelSelector(
                            selectedLevel = state.userProfile?.softPreferences?.spiceTolerance,
                            onLevelSelected = { level ->
                                val updatedProfile = state.userProfile?.copy(
                                    softPreferences = state.userProfile!!.softPreferences.copy(
                                        spiceTolerance = level
                                    )
                                )
                                updatedProfile?.let {
                                    viewModel.updateProfileData(it)
                                }
                            }
                        )

                        Spacer(Modifier.height(32.dp))

                        // Save button
                        Button(
                            onClick = {
                                viewModel.updateProfile()
                                Toast.makeText(context, "Preferences saved!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Text("Save Preferences")
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SpiceLevelSelector(
        selectedLevel: SpiceLevel?,
        onLevelSelected: (SpiceLevel) -> Unit
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SpiceLevel.values().forEach { level ->
                FilterChip(
                    selected = selectedLevel == level,
                    onClick = { onLevelSelected(level) },
                    label = { Text(level.name) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    @Composable
    private fun BottomNavigationBar(currentScreen: String) {
        val context = LocalContext.current

        // Exact match to ProfileActivity navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(Color(0xFFE8F5E9))
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // My Preferences
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .clickable { /* Already on Preferences */ }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "My Preferences",
                    fontSize = 14.sp,
                    fontWeight = if (currentScreen == "Preferences") FontWeight.Bold else FontWeight.Normal,
                    color = Color.Black
                )
            }

            // My Teams
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .clickable {
                        val intent = Intent(context, MyTeamsActivity::class.java)
                        context.startActivity(intent)
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "My Teams",
                    fontSize = 14.sp,
                    fontWeight = if (currentScreen == "MyTeams") FontWeight.Bold else FontWeight.Normal,
                    color = Color.Black
                )
            }

            // My Profile
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .clickable {
                        val intent = Intent(context, ProfileActivity::class.java)
                        context.startActivity(intent)
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "My Profile",
                    fontSize = 14.sp,
                    fontWeight = if (currentScreen == "Profile") FontWeight.Bold else FontWeight.Normal,
                    color = Color.Black
                )
            }
        }
    }
}
