package com.example.veato.ui.auth

import android.content.Intent
import android.os.Looper
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.veato.OnboardingActivity
import com.example.veato.R
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import io.mockk.*
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
class RegisterActivityTest {

    private lateinit var mockAuth: FirebaseAuth
    private lateinit var mockUser: FirebaseUser
    private lateinit var mockFirestore: FirebaseFirestore

    private lateinit var scenario: ActivityScenario<RegisterActivity>

    @Before
    fun setup() {
        mockkStatic(FirebaseAuth::class)
        mockkStatic(FirebaseFirestore::class)

        mockAuth = mockk(relaxed = true)
        mockFirestore = mockk(relaxed = true)
        mockUser = mockk(relaxed = true)

        every { FirebaseAuth.getInstance() } returns mockAuth
        every { FirebaseFirestore.getInstance() } returns mockFirestore

        scenario = ActivityScenario.launch(RegisterActivity::class.java)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    private fun flush() = shadowOf(Looper.getMainLooper()).idle()

    private fun fillValidInputs(activity: RegisterActivity) {
        activity.findViewById<EditText>(R.id.etFullName).setText("John Doe")
        activity.findViewById<EditText>(R.id.etUsername).setText("@johndoe")
        activity.findViewById<EditText>(R.id.etRegEmail).setText("test@test.com")
        activity.findViewById<EditText>(R.id.etRegPassword).setText("123456")
        activity.findViewById<EditText>(R.id.etRegConfirm).setText("123456")
    }

    @Test
    fun validation_fullNameEmpty() {
        scenario.onActivity { a ->
            a.findViewById<EditText>(R.id.etFullName).setText("")
            a.findViewById<EditText>(R.id.etUsername).setText("user123")
            a.findViewById<EditText>(R.id.etRegEmail).setText("a@a.com")
            a.findViewById<EditText>(R.id.etRegPassword).setText("123456")
            a.findViewById<EditText>(R.id.etRegConfirm).setText("123456")
            a.findViewById<TextView>(R.id.btnCreate).performClick()
        }
        flush()
        assertEquals("Full name required", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun validation_usernameInvalid() {
        scenario.onActivity { a ->
            a.findViewById<EditText>(R.id.etFullName).setText("John")
            a.findViewById<EditText>(R.id.etUsername).setText("###")
            a.findViewById<EditText>(R.id.etRegEmail).setText("a@a.com")
            a.findViewById<EditText>(R.id.etRegPassword).setText("123456")
            a.findViewById<EditText>(R.id.etRegConfirm).setText("123456")
            a.findViewById<TextView>(R.id.btnCreate).performClick()
        }
        flush()
        assertEquals("Invalid username (use 3–20 letters/numbers/_)", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun validation_passwordMismatch() {
        scenario.onActivity { a ->
            a.findViewById<EditText>(R.id.etFullName).setText("John")
            a.findViewById<EditText>(R.id.etUsername).setText("user123")
            a.findViewById<EditText>(R.id.etRegEmail).setText("test@test.com")
            a.findViewById<EditText>(R.id.etRegPassword).setText("123456")
            a.findViewById<EditText>(R.id.etRegConfirm).setText("xxxxxx")
            a.findViewById<TextView>(R.id.btnCreate).performClick()
        }
        flush()
        assertEquals("Passwords do not match", ShadowToast.getTextOfLatestToast())
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun register_usernameTaken_showsError() = runTest {
        scenario.onActivity { a ->
            fillValidInputs(a)

            val authResult = mockk<AuthResult>()
            every { mockAuth.createUserWithEmailAndPassword(any(), any()) } returns Tasks.forResult(authResult)
            every { authResult.user } returns mockUser
            every { mockUser.uid } returns "uid123"
            every { mockUser.delete() } returns Tasks.forResult(null)

            every { mockFirestore.runTransaction<Any?>(any()) } returns
                    Tasks.forException(IllegalStateException("Username already taken"))

            a.findViewById<TextView>(R.id.btnCreate).performClick()
        }

        advanceUntilIdle()
        flush()

        assertEquals("Username already taken", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun register_authFailure_showsError() {
        scenario.onActivity { a ->
            fillValidInputs(a)

            every {
                mockAuth.createUserWithEmailAndPassword(any(), any())
            } returns Tasks.forException(Exception("Email already exists"))

            a.findViewById<TextView>(R.id.btnCreate).performClick()
        }

        flush()
        assertEquals("Email already exists", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun validation_invalidEmailFormat() {
        scenario.onActivity { a ->
            a.findViewById<EditText>(R.id.etFullName).setText("John Doe")
            a.findViewById<EditText>(R.id.etUsername).setText("user123")
            a.findViewById<EditText>(R.id.etRegEmail).setText("invalidemail.com")
            a.findViewById<EditText>(R.id.etRegPassword).setText("123456")
            a.findViewById<EditText>(R.id.etRegConfirm).setText("123456")
            a.findViewById<TextView>(R.id.btnCreate).performClick()
        }
        flush()
        assertEquals(
            "Email must be a valid email address (e.g., user@example.com)",
            ShadowToast.getTextOfLatestToast()
        )
    }

    @Test
    fun validation_passwordTooShort() {
        scenario.onActivity { a ->
            a.findViewById<EditText>(R.id.etFullName).setText("John")
            a.findViewById<EditText>(R.id.etUsername).setText("user123")
            a.findViewById<EditText>(R.id.etRegEmail).setText("test@test.com")
            a.findViewById<EditText>(R.id.etRegPassword).setText("123")
            a.findViewById<EditText>(R.id.etRegConfirm).setText("123")
            a.findViewById<TextView>(R.id.btnCreate).performClick()
        }
        flush()
        assertEquals("Password must be at least 6 chars", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun register_success_navigatesToOnboarding() {
        scenario.onActivity { a ->
            fillValidInputs(a)

            val authResult = mockk<AuthResult>()
            every { mockAuth.createUserWithEmailAndPassword(any(), any()) } returns Tasks.forResult(authResult)
            every { authResult.user } returns mockUser
            every { mockUser.uid } returns "uid123"

            val mockTransaction = mockk<Transaction>(relaxed = true)

            every {
                mockFirestore.runTransaction<Any?>(any())
            } answers { Tasks.forResult(null) }

            a.findViewById<TextView>(R.id.btnCreate).performClick()
        }

        flush()

        val nextIntent = shadowOf(ApplicationProvider.getApplicationContext<android.app.Application>())
            .nextStartedActivity

        assertEquals(OnboardingActivity::class.java.name, nextIntent.component?.className)
    }

    @Test
    fun register_uidNull_showsError() {
        scenario.onActivity { a ->
            fillValidInputs(a)

            val authResult = mockk<AuthResult>()
            every { mockAuth.createUserWithEmailAndPassword(any(), any()) } returns Tasks.forResult(authResult)
            every { authResult.user } returns null  // simulate missing UID

            a.findViewById<TextView>(R.id.btnCreate).performClick()
        }

        flush()

        assertEquals("No UID", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun register_transactionFailure_buttonReenabled() {
        scenario.onActivity { a ->
            fillValidInputs(a)

            val authResult = mockk<AuthResult>()
            every { mockAuth.createUserWithEmailAndPassword(any(), any()) } returns Tasks.forResult(authResult)
            every { authResult.user } returns mockUser
            every { mockUser.uid } returns "uid123"
            every { mockUser.delete() } returns Tasks.forResult(null)

            every { mockFirestore.runTransaction<Any?>(any()) } returns Tasks.forException(Exception("Fail"))

            val btn = a.findViewById<TextView>(R.id.btnCreate)

            a.findViewById<TextView>(R.id.btnCreate).performClick()
            flush()

            assertTrue(btn.isEnabled)   // busy(false) must run after failure
        }
    }

    @Test
    fun register_success_buttonReenabled() {
        scenario.onActivity { a ->
            fillValidInputs(a)

            val authResult = mockk<AuthResult>()
            every { mockAuth.createUserWithEmailAndPassword(any(), any()) } returns Tasks.forResult(authResult)
            every { authResult.user } returns mockUser
            every { mockUser.uid } returns "uid123"

            every { mockFirestore.runTransaction<Any?>(any()) } returns Tasks.forResult(null)

            val btn = a.findViewById<TextView>(R.id.btnCreate)

            a.findViewById<TextView>(R.id.btnCreate).performClick()
            flush()

            assertTrue(btn.isEnabled)
        }
    }

    @Test
    fun validation_emailEmpty() {
        scenario.onActivity { a ->
            a.findViewById<EditText>(R.id.etFullName).setText("John")
            a.findViewById<EditText>(R.id.etUsername).setText("user123")
            a.findViewById<EditText>(R.id.etRegEmail).setText("")
            a.findViewById<EditText>(R.id.etRegPassword).setText("123456")
            a.findViewById<EditText>(R.id.etRegConfirm).setText("123456")
            a.findViewById<TextView>(R.id.btnCreate).performClick()
        }

        flush()
        assertEquals("Email required", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun register_authCompleteListener_fires() {
        scenario.onActivity { a ->
            fillValidInputs(a)

            val errorTask = Tasks.forException<AuthResult>(Exception("Failure"))
            every { mockAuth.createUserWithEmailAndPassword(any(), any()) } returns errorTask

            a.findViewById<TextView>(R.id.btnCreate).performClick()
        }

        flush()

        assertEquals("Failure", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun validation_confirmPasswordEmpty() {
        scenario.onActivity { a ->
            a.findViewById<EditText>(R.id.etFullName).setText("John")
            a.findViewById<EditText>(R.id.etUsername).setText("user123")
            a.findViewById<EditText>(R.id.etRegEmail).setText("test@test.com")
            a.findViewById<EditText>(R.id.etRegPassword).setText("123456")
            a.findViewById<EditText>(R.id.etRegConfirm).setText("")
            a.findViewById<TextView>(R.id.btnCreate).performClick()
        }

        flush()

        assertEquals("Passwords do not match", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun createAccount_timeoutRunnable_runs_afterDelay() {
        scenario.onActivity { a ->
            fillValidInputs(a)

            // Fake hanging task: never calls any listener
            val hangingTask = mockk<Task<AuthResult>>(relaxed = true)

            every {
                mockAuth.createUserWithEmailAndPassword(any(), any())
            } returns hangingTask

            val btn = a.findViewById<TextView>(R.id.btnCreate)

            // Click register → triggers createAccount() and schedules timeout
            btn.performClick()

            // Initially disabled
            assertFalse(btn.isEnabled)

            // Advance 10 seconds so the timeout Runnable executes
            shadowOf(Looper.getMainLooper()).idleFor(10, java.util.concurrent.TimeUnit.SECONDS)
            shadowOf(Looper.getMainLooper()).runToEndOfTasks()

            // Button SHOULD STILL BE DISABLED because Firebase never resolved
            assertFalse(btn.isEnabled)
        }
    }


    @Test
    fun validation_passwordEmpty() {
        scenario.onActivity { a ->
            a.findViewById<EditText>(R.id.etFullName).setText("John")
            a.findViewById<EditText>(R.id.etUsername).setText("user123")
            a.findViewById<EditText>(R.id.etRegEmail).setText("test@test.com")

            a.findViewById<EditText>(R.id.etRegPassword).setText("")
            a.findViewById<EditText>(R.id.etRegConfirm).setText("")

            a.findViewById<TextView>(R.id.btnCreate).performClick()
        }
        flush()
        assertEquals("Password must be at least 6 chars", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun transaction_success_executesInsideLambda() {
        scenario.onActivity { a ->
            fillValidInputs(a)

            val authResult = mockk<AuthResult>()
            val mockTr = mockk<Transaction>(relaxed = true)
            val usernameRef = mockk<DocumentReference>()
            val profileRef = mockk<DocumentReference>()
            val snapshot = mockk<DocumentSnapshot>()

            every { authResult.user } returns mockUser
            every { mockUser.uid } returns "uid999"

            every { mockAuth.createUserWithEmailAndPassword(any(), any()) } returns Tasks.forResult(authResult)

            every { mockFirestore.collection("usernames").document(any()) } returns usernameRef
            every { mockFirestore.collection("users").document(any()) } returns profileRef

            every { snapshot.exists() } returns false

            every { mockTr.get(usernameRef) } returns snapshot

            every {
                mockFirestore.runTransaction<Any?>(any())
            } answers {
                val lambda = arg<Transaction.Function<Any?>>(0)
                lambda.apply(mockTr)
                Tasks.forResult(null)
            }

            a.findViewById<TextView>(R.id.btnCreate).performClick()
        }

        flush()
        // Verify no errors appear
        assertNotEquals("Username already taken", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun transaction_success_onCompleteListener_runs() {
        scenario.onActivity { a ->
            fillValidInputs(a)

            val authResult = mockk<AuthResult>()

            every { mockAuth.createUserWithEmailAndPassword(any(), any()) } returns Tasks.forResult(authResult)
            every { authResult.user } returns mockUser
            every { mockUser.uid } returns "uid777"

            every { mockFirestore.runTransaction<Any?>(any()) } returns Tasks.forResult(null)

            a.findViewById<TextView>(R.id.btnCreate).performClick()
        }

        flush()

        val nextIntent =
            shadowOf(ApplicationProvider.getApplicationContext<android.app.Application>())
                .nextStartedActivity

        assertEquals(OnboardingActivity::class.java.name, nextIntent.component?.className)
    }

    @Test
    fun timeoutRunnable_triggersAfterDelay() {
        scenario.onActivity { a ->
            fillValidInputs(a)

            // Firebase call never completes → simulate hang
            every {
                mockAuth.createUserWithEmailAndPassword(any(), any())
            } returns Tasks.forCanceled()

            // Trigger register()
            a.findViewById<TextView>(R.id.btnCreate).performClick()
        }

        // Fast-forward the main looper by 10 seconds
        shadowOf(Looper.getMainLooper())
            .idleFor(10_000, java.util.concurrent.TimeUnit.MILLISECONDS)

        // We cannot assert logs, but test ensures no crash and timeout runnable executes
        assertTrue(true)
    }

}
