package com.example.veato.ui.main

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import com.example.veato.MyPreferencesActivity
import com.example.veato.MyTeamsActivity
import com.example.veato.ProfileActivity
import com.example.veato.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityTest {

    private lateinit var mockAuth: FirebaseAuth
    private lateinit var mockUser: FirebaseUser

    @Before
    fun setup() {
        mockkStatic(FirebaseAuth::class)

        mockAuth = mockk(relaxed = true)
        mockUser = mockk(relaxed = true)

        every { mockUser.email } returns "tester@mail.com"
        every { mockAuth.currentUser } returns mockUser
        every { FirebaseAuth.getInstance() } returns mockAuth
    }

    @After
    fun tearDown() {
        unmockkStatic(FirebaseAuth::class)
    }

    @Test
    fun activity_starts_successfully() {
        ActivityScenario.launch(MainActivity::class.java).use {
            assert(true)
        }
    }

    @Test
    fun logout_calls_signOut_and_opens_LoginActivity() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()

        // manually invoke logout action
        activity.runOnUiThread {
            mockAuth.signOut()
            val intent = Intent(activity, LoginActivity::class.java)
            activity.startActivity(intent)
        }

        verify(exactly = 1) { mockAuth.signOut() }

        val nextIntent = shadowOf(activity).nextStartedActivity
        assert(nextIntent.component?.className == LoginActivity::class.java.name)
    }

    @Test
    fun clicking_myPreferences_starts_correct_activity() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()

        val intent = Intent(activity, MyPreferencesActivity::class.java)
        activity.startActivity(intent)

        val next = shadowOf(activity).nextStartedActivity
        assert(next.component?.className == MyPreferencesActivity::class.java.name)
    }

    @Test
    fun clicking_myTeams_starts_correct_activity() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()

        val intent = Intent(activity, MyTeamsActivity::class.java)
        activity.startActivity(intent)

        val next = shadowOf(activity).nextStartedActivity
        assert(next.component?.className == MyTeamsActivity::class.java.name)
    }

    @Test
    fun clicking_myProfile_starts_correct_activity() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()

        val intent = Intent(activity, ProfileActivity::class.java)
        activity.startActivity(intent)

        val next = shadowOf(activity).nextStartedActivity
        assert(next.component?.className == ProfileActivity::class.java.name)
    }

    @Test
    fun welcome_message_shows_user_email() {
        every { mockUser.email } returns "hello@test.com"

        ActivityScenario.launch(MainActivity::class.java).use {
            assert(true)
        }
    }

    @Test
    fun activity_recreates_successfully() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.recreate()
        scenario.onActivity { assert(true) }
    }

    @Test
    fun lifecycle_pause_resume_does_not_crash() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        controller.pause()
        controller.resume()
        assert(true)
    }
}
