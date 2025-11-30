package com.example.veato

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class PreferencesIntegrationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MyPreferencesActivity>()

    private lateinit var auth: FirebaseAuth

    @Before
    fun setUp() {
        // Initialize Firebase
        try {
            FirebaseApp.getInstance()
        } catch (e: IllegalStateException) {
            FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext())
        }

        // Get FirebaseAuth instance
        auth = FirebaseAuth.getInstance()

        // Sign in with test account if not logged in
        if (auth.currentUser == null) {
            val email = "abcdef@example.com"
            val password = "abcdef"

            val signInTask = auth.signInWithEmailAndPassword(email, password)
            Tasks.await(signInTask)
            assertTrue("FirebaseAuth signIn failed in @Before", signInTask.isSuccessful)
        }

        // Initialize Espresso Intents
        Intents.init()

        // Recreate activity to ensure it sees the authenticated user
        composeRule.activityRule.scenario.recreate()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    /**
     * Verifies that the top app bar shows the correct title.
     */
    @Test
    fun topAppBar_displaysMyPreferencesTitle() {
        val topBarTitleMatcher = hasText("My Preferences") and !hasClickAction()

        composeRule.onNode(topBarTitleMatcher)
            .assertIsDisplayed()
    }


    /**
     * Clicking "My Teams" navigates to MyTeamsActivity.
     */
    @Test
    fun clickingMyTeams_navigatesToMyTeamsActivity() {
        composeRule.onNodeWithText("My Teams")
            .assertIsDisplayed()
            .performClick()

        intended(hasComponent(MyTeamsActivity::class.java.name))
    }

    /**
     * Clicking "My Profile" navigates to ProfileActivity.
     */
    @Test
    fun clickingMyProfile_navigatesToProfileActivity() {
        composeRule.onNodeWithText("My Profile")
            .assertIsDisplayed()
            .performClick()

        intended(hasComponent(ProfileActivity::class.java.name))
    }


    // Favorite cuisine chips are visible when profile is loaded
    @Test
    fun favoriteCuisine_showsKoreanAndJapanese() {
        composeRule.onNodeWithText("Favorite Cuisines").assertIsDisplayed()

        composeRule.onNodeWithText("KOREAN").assertIsDisplayed()
        composeRule.onNodeWithText("JAPANESE").assertIsDisplayed()
    }

    // Spice tolerance chips are visible
    @Test
    fun spiceTolerance_showsMainLevels() {
        composeRule.onNodeWithText("Spice Tolerance").assertIsDisplayed()

        composeRule.onNodeWithText("NONE").assertIsDisplayed()
        composeRule.onNodeWithText("LOW").assertIsDisplayed()
        composeRule.onNodeWithText("MEDIUM").assertIsDisplayed()
        composeRule.onNodeWithText("HIGH").assertIsDisplayed()
    }

    // Save button is visible together with preference sections
    @Test
    fun savePreferences_showsWithSections() {
        composeRule.onNodeWithText("Soft Preferences").assertIsDisplayed()
        composeRule.onNodeWithText("Favorite Cuisines").assertIsDisplayed()
        composeRule.onNodeWithText("Spice Tolerance").assertIsDisplayed()

        composeRule.onNodeWithText("Save Preferences").assertIsDisplayed()
    }


}
