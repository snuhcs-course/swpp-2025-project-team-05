package com.example.veato

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.veato.data.local.ProfileDataStoreImpl
import com.example.veato.data.repository.UserProfileRepositoryImpl
import com.example.veato.ui.onboarding.OnboardingViewModel
import com.example.veato.ui.onboarding.OnboardingViewModelFactory
import com.example.veato.ui.onboarding.screens.*
import com.example.veato.ui.theme.VeatoTheme
import com.example.veato.ui.main.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.veato.data.remote.ProfileApiDataSource
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class OnboardingActivity : ComponentActivity() {
    private val db by lazy { Firebase.firestore }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VeatoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OnboardingApp(
                        onComplete = { profile ->
                            // Save to Firebase and navigate to main app
                            lifecycleScope.launch {
                                saveProfileToFirebase(profile)
                            }
                        }
                    )
                }
            }
        }
    }

    private suspend fun saveProfileToFirebase(profile: com.example.veato.data.model.UserProfile) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        try {
            // Convert profile to Firebase-friendly map
            // Convert enums to strings and sets to lists
            val profileData = hashMapOf(
                "onboardingCompleted" to true,
                "lastUpdated" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                // Hard Constraints - convert sets to lists of strings
                "dietaryRestrictions" to profile.hardConstraints.dietaryRestrictions.map { it.name },
                "allergies" to profile.hardConstraints.allergies.map { it.name },
                "avoidIngredients" to profile.hardConstraints.avoidIngredients.toList(),
                "budgetCap" to profile.hardConstraints.budgetCap,
                // Soft Preferences - convert enums to strings
                "favoriteCuisines" to profile.softPreferences.favoriteCuisines.map { it.name },
                "spiceTolerance" to profile.softPreferences.spiceTolerance.name,
                "heavinessPreference" to profile.softPreferences.heavinessPreference.name,
                "typicalBudgetRange" to hashMapOf(
                    "minPrice" to profile.softPreferences.typicalBudgetRange.minPrice,
                    "maxPrice" to profile.softPreferences.typicalBudgetRange.maxPrice
                ),
                "mealTypePreferences" to profile.softPreferences.mealTypePreferences.map { it.name },
                "portionPreference" to (profile.softPreferences.portionPreference?.name ?: "MEDIUM")
            )

            db.collection("users").document(userId)
                .set(profileData, com.google.firebase.firestore.SetOptions.merge())
                .await()

            // Navigate to main app after successful save
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } catch (e: Exception) {
            // Show error and stay on current screen
            runOnUiThread {
                Toast.makeText(this, "Failed to save profile: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

@Composable
fun OnboardingApp(onComplete: (com.example.veato.data.model.UserProfile) -> Unit) {
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "demo_user"

    // Initialize repository and ViewModel properly in Compose
    val viewModel: OnboardingViewModel = viewModel(
        factory = OnboardingViewModelFactory(
            repository = UserProfileRepositoryImpl(
                ProfileDataStoreImpl(context),
                ProfileApiDataSource()
            ),
            userId = userId
        )
    )

    val state by viewModel.state.collectAsState()
    val profile = state.profileDraft

    // Listen for completion
    LaunchedEffect(state.isComplete) {
        if (state.isComplete) {
            onComplete(profile)
        }
    }

    // Show screens based on current state
    when (state.currentScreen) {
        is com.example.veato.ui.onboarding.OnboardingScreen.Welcome -> {
            WelcomeScreen(
                onNext = { viewModel.nextScreen() }
            )
        }

        is com.example.veato.ui.onboarding.OnboardingScreen.DietaryRestrictions -> {
            DietaryRestrictionsScreen(
                selectedRestrictions = profile.hardConstraints.dietaryRestrictions,
                onUpdate = { viewModel.updateDietaryRestrictions(it) },
                currentStep = state.currentStepNumber,
                totalSteps = state.totalSteps,
                onNext = { viewModel.nextScreen() },
                onPrevious = { viewModel.previousScreen() }
            )
        }

        is com.example.veato.ui.onboarding.OnboardingScreen.Allergies -> {
            AllergiesScreen(
                selectedAllergies = profile.hardConstraints.allergies,
                onUpdate = { viewModel.updateAllergies(it) },
                currentStep = state.currentStepNumber,
                totalSteps = state.totalSteps,
                onNext = { viewModel.nextScreen() },
                onPrevious = { viewModel.previousScreen() }
            )
        }

        is com.example.veato.ui.onboarding.OnboardingScreen.AvoidIngredients -> {
            AvoidIngredientsScreen(
                avoidList = profile.hardConstraints.avoidIngredients,
                onUpdate = { viewModel.updateAvoidIngredients(it) },
                currentStep = state.currentStepNumber,
                totalSteps = state.totalSteps,
                onNext = { viewModel.nextScreen() },
                onPrevious = { viewModel.previousScreen() }
            )
        }

        is com.example.veato.ui.onboarding.OnboardingScreen.BudgetCap -> {
            BudgetCapScreen(
                budgetCap = profile.hardConstraints.budgetCap,
                onUpdate = { viewModel.updateBudgetCap(it) },
                currentStep = state.currentStepNumber,
                totalSteps = state.totalSteps,
                onNext = { viewModel.nextScreen() },
                onPrevious = { viewModel.previousScreen() }
            )
        }

        is com.example.veato.ui.onboarding.OnboardingScreen.FavoriteCuisines -> {
            FavoriteCuisinesScreen(
                selectedCuisines = profile.softPreferences.favoriteCuisines,
                onUpdate = { viewModel.updateFavoriteCuisines(it) },
                currentStep = state.currentStepNumber,
                totalSteps = state.totalSteps,
                onNext = { viewModel.nextScreen() },
                onPrevious = { viewModel.previousScreen() }
            )
        }

        is com.example.veato.ui.onboarding.OnboardingScreen.SpiceTolerance -> {
            SpiceToleranceScreen(
                spiceLevel = profile.softPreferences.spiceTolerance,
                onUpdate = { viewModel.updateSpiceTolerance(it) },
                currentStep = state.currentStepNumber,
                totalSteps = state.totalSteps,
                onNext = { viewModel.nextScreen() },
                onPrevious = { viewModel.previousScreen() }
            )
        }

        is com.example.veato.ui.onboarding.OnboardingScreen.HeavinessPreference -> {
            HeavinessPreferenceScreen(
                heavinessLevel = profile.softPreferences.heavinessPreference,
                onUpdate = { viewModel.updateHeavinessPreference(it) },
                currentStep = state.currentStepNumber,
                totalSteps = state.totalSteps,
                onNext = { viewModel.nextScreen() },
                onPrevious = { viewModel.previousScreen() }
            )
        }

        is com.example.veato.ui.onboarding.OnboardingScreen.BudgetPreference -> {
            BudgetPreferenceScreen(
                budgetRange = profile.softPreferences.typicalBudgetRange,
                onUpdate = { viewModel.updateBudgetRange(it) },
                currentStep = state.currentStepNumber,
                totalSteps = state.totalSteps,
                onNext = { viewModel.nextScreen() },
                onPrevious = { viewModel.previousScreen() }
            )
        }

        is com.example.veato.ui.onboarding.OnboardingScreen.AdvancedPreferences -> {
            AdvancedPreferencesScreen(
                mealTypes = profile.softPreferences.mealTypePreferences,
                portionSize = profile.softPreferences.portionPreference,
                onUpdateMealTypes = { viewModel.updateMealTypePreferences(it) },
                onUpdatePortionSize = { viewModel.updatePortionPreference(it) },
                currentStep = state.currentStepNumber,
                totalSteps = state.totalSteps,
                onNext = { viewModel.nextScreen() },
                onPrevious = { viewModel.previousScreen() }
            )
        }

        is com.example.veato.ui.onboarding.OnboardingScreen.Summary -> {
            SummaryScreen(
                profile = profile,
                onSave = { viewModel.saveProfile() },
                onEdit = { step -> /* Navigate to step */ },
                currentStep = state.currentStepNumber,
                totalSteps = state.totalSteps,
                isSaving = state.isSaving,
                saveError = state.saveError
            )
        }
    }
}
