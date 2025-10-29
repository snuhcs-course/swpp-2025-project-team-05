package com.example.veato

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class LoginIntegrationTest {

    private lateinit var auth: FirebaseAuth

    @Before
    fun setup() {
        // Initialize Firebase
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext())
        auth = FirebaseAuth.getInstance()
    }

    @Test
    fun createOrLoginUser_success() = runTest {
        val email = "testuser@example.com"
        val password = "test1234"

        try {
            // Try to log in if user already exists
            val loginResult = auth.signInWithEmailAndPassword(email, password).await()
            assertThat(loginResult.user).isNotNull()
            println("Existing test user logged in successfully.")
        } catch (e: Exception) {
            println("User not found, creating new user.")
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            assertThat(result.user).isNotNull()
            assertThat(result.user?.email).isEqualTo(email)
        }
    }

    @Test
    fun loginWithWrongPassword_fails() = runTest {
        val email = "testuser@example.com"
        val wrongPassword = "wrongpass"

        var failed = false
        try {
            auth.signInWithEmailAndPassword(email, wrongPassword).await()
            assert(false) { "Expected login failure for wrong password, but succeeded." }
        } catch (e: Exception) {
            failed = true
            println("Login failed as expected for wrong password. Actual message: ${e.message}")
            assertThat(e).isInstanceOf(Exception::class.java)
        }

        assertThat(failed).isTrue()
    }

    @Test
    fun loginWithInvalidEmailFormat_fails() = runTest {
        val invalidEmail = "not-an-email"
        val password = "123456"

        try {
            auth.createUserWithEmailAndPassword(invalidEmail, password).await()
            assert(false) { "Expected failure for invalid email" }
        } catch (e: Exception) {
            println("Caught expected invalid email error.")
            assertThat(e.message).contains("badly formatted")
        }
    }

    @Test
    fun createDuplicateUser_failsWithCollisionException() = runTest {
        val email = "duplicateuser@example.com"
        val password = "dup1234"

        try {
            // First creation
            auth.createUserWithEmailAndPassword(email, password).await()
            // Try again with same email
            auth.createUserWithEmailAndPassword(email, password).await()
            assert(false) { "Expected FirebaseAuthUserCollisionException" }
        } catch (e: Exception) {
            println("Duplicate user creation failed as expected.")
            assertThat(e.javaClass.simpleName).contains("FirebaseAuthUserCollisionException")
        }
    }

    @Test
    fun signOutAndReLogin_success() = runTest {
        val email = "testuser@example.com"
        val password = "test1234"

        val user = auth.signInWithEmailAndPassword(email, password).await().user
        assertThat(user).isNotNull()
        println("Logged in as ${user?.email}")

        // Sign out
        auth.signOut()
        assertThat(auth.currentUser).isNull()
        println("Signed out successfully.")

        // Re-login
        val reLogin = auth.signInWithEmailAndPassword(email, password).await()
        assertThat(reLogin.user?.email).isEqualTo(email)
        println("Re-login succeeded.")
    }
}
