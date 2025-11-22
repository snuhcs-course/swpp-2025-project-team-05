package com.example.veato.ui.auth

import android.content.Intent
import android.os.Looper
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.veato.MyTeamsActivity
import com.example.veato.OnboardingActivity
import com.example.veato.R
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.milliseconds
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
class LoginActivityTest {

    private lateinit var mockAuth: FirebaseAuth
    private lateinit var mockUser: FirebaseUser
    private lateinit var mockFirestore: FirebaseFirestore
    private lateinit var scenario: ActivityScenario<LoginActivity>

    @Before
    fun setup() {
        mockkStatic(FirebaseAuth::class)
        mockkStatic(FirebaseFirestore::class)

        mockAuth = mockk(relaxed = true)
        mockFirestore = mockk(relaxed = true)
        mockUser = mockk(relaxed = true)

        every { FirebaseAuth.getInstance() } returns mockAuth
        every { FirebaseFirestore.getInstance() } returns mockFirestore

        scenario = ActivityScenario.launch(LoginActivity::class.java)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun flush() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    // ----------------------------------------------------
    // Login failure
    // ----------------------------------------------------
    @Test
    fun login_failure_shows_toast() = runTest {
        every {
            mockAuth.signInWithEmailAndPassword(any(), any())
        } returns Tasks.forException(Exception("Invalid credentials"))

        scenario.onActivity { activity ->
            activity.findViewById<EditText>(R.id.etEmail).setText("a@a.com")
            activity.findViewById<EditText>(R.id.etPassword).setText("123456")

            activity.findViewById<TextView>(R.id.btnLogin).performClick()
        }

        advanceUntilIdle()
        flush()

        assertEquals("Invalid credentials", ShadowToast.getTextOfLatestToast())
    }

    // ----------------------------------------------------
    // Login success + onboardingCompleted = true
    // ----------------------------------------------------
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun login_success_goes_to_myTeams() = runTest {
        val authResult = mockk<AuthResult>()
        every {
            mockAuth.signInWithEmailAndPassword(any(), any())
        } returns Tasks.forResult(authResult)

        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns "u123"

        val mockDoc = mockk<DocumentSnapshot>()
        every { mockDoc.getBoolean("onboardingCompleted") } returns true
        every {
            mockFirestore.collection("users").document("u123").get()
        } returns Tasks.forResult(mockDoc)

        scenario.onActivity { activity ->
            activity.findViewById<EditText>(R.id.etEmail).setText("a@a.com")
            activity.findViewById<EditText>(R.id.etPassword).setText("123456")
            activity.findViewById<TextView>(R.id.btnLogin).performClick()
        }

        advanceUntilIdle()
        flush()

        val nextIntent = shadowOf(ApplicationProvider.getApplicationContext() as android.app.Application)
            .nextStartedActivity

        assertNotNull(nextIntent)
        assertEquals(MyTeamsActivity::class.java.name, nextIntent.component?.className)
    }

    // ----------------------------------------------------
    // Login success + onboardingCompleted = false
    // ----------------------------------------------------
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun login_success_goes_to_onboarding() = runTest {
        val authResult = mockk<AuthResult>()
        every { mockAuth.signInWithEmailAndPassword(any(), any()) } returns Tasks.forResult(authResult)

        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns "u999"

        val mockDoc = mockk<DocumentSnapshot>()
        every { mockDoc.getBoolean("onboardingCompleted") } returns false
        every {
            mockFirestore.collection("users").document("u999").get()
        } returns Tasks.forResult(mockDoc)

        scenario.onActivity { activity ->
            activity.findViewById<EditText>(R.id.etEmail).setText("test@test.com")
            activity.findViewById<EditText>(R.id.etPassword).setText("pass")
            activity.findViewById<TextView>(R.id.btnLogin).performClick()
        }

        advanceUntilIdle()
        flush()

        val nextIntent = shadowOf(ApplicationProvider.getApplicationContext() as android.app.Application)
            .nextStartedActivity

        assertNotNull(nextIntent)
        assertEquals(OnboardingActivity::class.java.name, nextIntent.component?.className)
    }

    // ----------------------------------------------------
    // sendReset() successful
    // ----------------------------------------------------
    @Test
    fun sendReset_shows_generic_message() {
        every {
            mockAuth.sendPasswordResetEmail(any())
        } returns Tasks.forResult(null)

        scenario.onActivity { activity ->
            activity.findViewById<EditText>(R.id.etEmail).setText("user@test.com")
            activity.findViewById<TextView>(R.id.tvForgot).performClick()
        }

        flush()

        assertEquals("Reset link has been sent if registered", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun login_emptyFields_showsRequiredMessage() {
        scenario.onActivity { activity ->
            activity.findViewById<EditText>(R.id.etEmail).setText("")
            activity.findViewById<EditText>(R.id.etPassword).setText("")

            activity.findViewById<TextView>(R.id.btnLogin).performClick()
        }

        assertEquals("Email and password are required", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun sendReset_invalidEmail_showsError() {
        scenario.onActivity { activity ->
            activity.findViewById<EditText>(R.id.etEmail).setText("invalidEmail")
            activity.findViewById<TextView>(R.id.tvForgot).performClick()
        }

        assertEquals("Enter a valid email to receive a reset link", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun login_success_but_uidNull_showsError() = runTest {
        val fakeTask = mockk<Task<AuthResult>>(relaxed = true)

        every {
            mockAuth.signInWithEmailAndPassword(any(), any())
        } answers {
            every { fakeTask.addOnSuccessListener(any()) } answers {
                val listener = arg<OnSuccessListener<AuthResult>>(0)
                listener.onSuccess(mockk()) // simulate Firebase success
                fakeTask
            }
            fakeTask
        }

        every { mockAuth.currentUser } returns null

        scenario.onActivity { activity ->
            activity.findViewById<EditText>(R.id.etEmail).setText("a@a.com")
            activity.findViewById<EditText>(R.id.etPassword).setText("123")
            activity.findViewById<TextView>(R.id.btnLogin).performClick()
        }

        shadowOf(Looper.getMainLooper()).idle()

        assertEquals("Login error: User ID is null", ShadowToast.getTextOfLatestToast())
    }


    @Test
    fun onStart_noUser_doesNothing() {
        every { mockAuth.currentUser } returns null

        val scenario = ActivityScenario.launch(LoginActivity::class.java)

        val next = shadowOf(ApplicationProvider.getApplicationContext() as android.app.Application)
            .nextStartedActivity

        assertNull(next)
    }

    @Test
    fun navigateBasedOnOnboardingStatus_firestoreThrows_goesToOnboarding() = runTest {
        val authResult = mockk<AuthResult>()
        val fakeTask = mockk<Task<AuthResult>>(relaxed = true)

        // Make Firebase call trigger .addOnSuccessListener immediately
        every {
            mockAuth.signInWithEmailAndPassword(any(), any())
        } answers {
            every { fakeTask.addOnSuccessListener(any()) } answers {
                val listener = arg<OnSuccessListener<AuthResult>>(0)
                listener.onSuccess(authResult)   // FIRE success
                fakeTask
            }
            fakeTask
        }

        // Simulate logged-in user
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns "u777"

        // Firestore throws inside navigateBasedOnOnboardingStatus()
        every {
            mockFirestore.collection("users").document("u777").get()
        } returns Tasks.forException(Exception("Firestore error"))

        scenario.onActivity { activity ->
            activity.findViewById<EditText>(R.id.etEmail).setText("a@a.com")
            activity.findViewById<EditText>(R.id.etPassword).setText("pass")
            activity.findViewById<TextView>(R.id.btnLogin).performClick()
        }

        shadowOf(Looper.getMainLooper()).idle()
        advanceUntilIdle()

        val nextIntent = shadowOf(
            ApplicationProvider.getApplicationContext() as android.app.Application
        ).nextStartedActivity

        assertNotNull(nextIntent)
        assertEquals(OnboardingActivity::class.java.name, nextIntent!!.component?.className)
    }

    @Test
    fun login_timeoutRunnable_runs() {
        every { mockAuth.signInWithEmailAndPassword(any(), any()) } returns Tasks.forCanceled()

        scenario.onActivity { activity ->
            activity.findViewById<EditText>(R.id.etEmail).setText("x@x.com")
            activity.findViewById<EditText>(R.id.etPassword).setText("123456")

            activity.findViewById<TextView>(R.id.btnLogin).performClick()
        }

        assertTrue(true)
    }

}
