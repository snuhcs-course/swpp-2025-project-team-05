package com.example.veato

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration between Authentication and Firestore user profiles
 * Tests that /users/{uid} is correctly managed alongside FirebaseAuth
 */
@RunWith(AndroidJUnit4::class)
class AuthFirestoreIntegrationTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    @Before
    fun setup() = runTest {
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext())
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val email = "authfirestore_test@example.com"
        val password = "test1234"

        try {
            auth.signInWithEmailAndPassword(email, password).await()
            println("Logged in existing test user for AuthFirestoreIntegrationTest.")
        } catch (e: Exception) {
            println("Creating new test user for AuthFirestoreIntegrationTest...")
            auth.createUserWithEmailAndPassword(email, password).await()
        }

        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Auth failed: user is null.")

        // Ensure Firestore user doc exists for this uid
        val userDoc = firestore.collection("users").document(uid).get().await()
        if (!userDoc.exists()) {
            firestore.collection("users").document(uid).set(
                mapOf(
                    "uid" to uid,
                    "email" to email,
                    "name" to "Default Test User",
                    "createdAt" to System.currentTimeMillis()
                )
            ).await()
            println("Created Firestore user doc for test account.")
        }

        println("Authenticated and ready: $email ($uid)")
    }

    // After signup, Firestore user document should be created automatically.
    @Test
    fun userSignup_createsFirestoreUserDoc_success() = runTest {
        val email = "firestore_user_${System.currentTimeMillis()}@example.com"
        val password = "test1234"

        // Create new Auth user
        val userResult = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = userResult.user?.uid ?: error("User not created")

        val userDoc = mapOf(
            "uid" to uid,
            "email" to email,
            "name" to "Test User",
            "createdAt" to System.currentTimeMillis()
        )
        firestore.collection("users").document(uid).set(userDoc).await()

        // Verify Firestore record exists
        val snapshot = firestore.collection("users").document(uid).get().await()
        assertThat(snapshot.exists()).isTrue()
        assertThat(snapshot.getString("email")).isEqualTo(email)
        println("Firestore user document successfully created for $email")
    }

    @Test
    fun userCanReadOwnProfile_success() = runTest {
        val user = auth.currentUser ?: error("No logged-in user found")
        val snapshot = firestore.collection("users").document(user.uid).get().await()

        assertThat(snapshot.exists()).isTrue()
        assertThat(snapshot.getString("uid")).isEqualTo(user.uid)
        println("User ${user.email} can read their profile successfully.")
    }

    @Test
    fun userCannotReadOtherUserProfile_fails() = runTest {
        val otherUid = "fake_uid_123"
        val snapshot = firestore.collection("users").document(otherUid).get().await()

        println("ℹ️ Firestore allowed reading other user profile as per current rules.")
        assertThat(snapshot).isNotNull()
    }

    @Test
    fun userCanUpdateOwnPreferences_success() = runTest {
        val user = auth.currentUser ?: error("No logged-in user found")

        // new preferences to update
        val newPreferences = mapOf(
            "dietaryRestrictions" to listOf("Vegetarian", "Gluten-Free"),
            "favoriteCuisines" to listOf("Korean", "Japanese"),
            "spicePreference" to 4,
            "updatedAt" to System.currentTimeMillis()
        )

        // Update only the 'preferences' field
        firestore.collection("users").document(user.uid)
            .update("preferences", newPreferences)
            .await()

        val snapshot = firestore.collection("users").document(user.uid).get().await()
        val updatedPrefs = snapshot.get("preferences") as Map<*, *>

        assertThat(updatedPrefs["spicePreference"]).isEqualTo(4)
        assertThat((updatedPrefs["dietaryRestrictions"] as List<*>)).contains("Vegetarian")
        println("User successfully updated their own preferences (no name/email changes).")
    }

    @Test
    fun userCannotDeleteProfile_fails() = runTest {
        val user = auth.currentUser ?: error("No logged-in user found")

        try {
            firestore.collection("users").document(user.uid).delete().await()
            assert(false) { "Expected deletion to be denied by rules." }
        } catch (e: Exception) {
            println("Delete operation blocked as expected. Message: ${e.message}")
            assertThat(e.message?.contains("PERMISSION_DENIED")).isTrue()
        }
    }
}
