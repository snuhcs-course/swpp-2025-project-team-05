package com.example.veato

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Profile Integration Tests
 * - Verifies profile setup (onboarding) and profile update persistence.
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class ProfileIntegrationTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var uid: String

    @Before
    fun setup() = runTest {
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext())
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Reusable account for integration testing
        val email = "profile_test@example.com"
        val password = "profile1234"

        try {
            auth.signInWithEmailAndPassword(email, password).await()
        } catch (e: Exception) {
            auth.createUserWithEmailAndPassword(email, password).await()
        }

        uid = auth.currentUser?.uid ?: error("No user authenticated")
        println("Logged in as test user $uid")
    }

    // Simulates onboarding save process (from OnboardingActivity.saveProfileToFirebase)
    @Test
    fun profileSetup_savesToFirestore_success() = runTest {
        val profileData = mapOf(
            "onboardingCompleted" to true,
            "dietaryRestrictions" to listOf("VEGETARIAN"),
            "allergies" to listOf("PEANUTS"),
            "avoidIngredients" to listOf("Shellfish"),
            "favoriteCuisines" to listOf("KOREAN", "JAPANESE"),
            "spiceTolerance" to "LEVEL_3",
            "mealTypePreferences" to listOf("RICE_BASED"),
            "portionPreference" to "MEDIUM"
        )

        firestore.collection("users").document(uid).set(profileData).await()

        val snapshot = firestore.collection("users").document(uid).get().await()
        assertThat(snapshot.exists()).isTrue()
        assertThat(snapshot.getBoolean("onboardingCompleted")).isTrue()
        assertThat(snapshot.get("favoriteCuisines")).isEqualTo(listOf("KOREAN", "JAPANESE"))
        println("Onboarding profile saved successfully to Firestore.")
    }

    // Simulates updating user preferences (from ProfileActivity → ProfileViewModel.updateProfile)
    @Test
    fun profileUpdate_changesPersisted_success() = runTest {
        val updates = mapOf(
            "favoriteCuisines" to listOf("CHINESE", "FUSION"),
            "spiceTolerance" to "LEVEL_5",
            "dietaryRestrictions" to listOf("HALAL", "GLUTEN_FREE")
        )

        firestore.collection("users").document(uid).update(updates).await()

        val snapshot = firestore.collection("users").document(uid).get().await()
        assertThat(snapshot.get("favoriteCuisines")).isEqualTo(listOf("CHINESE", "FUSION"))
        assertThat(snapshot.getString("spiceTolerance")).isEqualTo("LEVEL_5")
        println("Profile update changes persisted correctly in Firestore.")
    }

    @Test
    fun profileUpdate_nullField_storedAsNull_success() = runTest {
        val update = mapOf("portionPreference" to null)

        firestore.collection("users").document(uid).update(update).await()
        val snapshot = firestore.collection("users").document(uid).get().await()

        // Firestore will store null unless rules block it
        assertThat(snapshot.exists()).isTrue()
        assertThat(snapshot.contains("portionPreference")).isTrue()
        assertThat(snapshot.get("portionPreference")).isNull()

        println("Firestore accepted null field update and stored it as null.")
    }
}
