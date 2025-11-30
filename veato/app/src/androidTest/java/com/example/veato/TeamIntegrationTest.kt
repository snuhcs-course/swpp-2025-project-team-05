package com.example.veato

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import junit.framework.AssertionFailedError
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Integration tests for the Teams flow.
 *
 * These tests verify:
 *  - MyTeamsActivity UI loads correctly
 *  - "Create new team" dialog opens and closes
 *  - BottomNavigationBar navigates to correct screens
 *  - Basic visibility and interaction flow
 *
 * Note: These tests do NOT verify Firestore behavior.
 *       Firestore-related E2E tests should be handled separately.
 */
@RunWith(AndroidJUnit4::class)
@MediumTest
class TeamIntegrationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MyTeamsActivity>()

    private lateinit var auth: FirebaseAuth

    // Shared test team name used by TeamDetail tests.
    // Precondition: a team card with this exact title is visible in MyTeamsActivity.
    private val testTeamName = "Integration Test Team"

    @Before
    fun setUp() {
        // 1) Initialize Firebase
        try {
            FirebaseApp.getInstance()
        } catch (e: IllegalStateException) {
            FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext())
        }

        // 2) Get FirebaseAuth instance
        auth = FirebaseAuth.getInstance()

        // 3) If not logged in, sign in with the test account
        if (auth.currentUser == null) {
            val email = "abcdef@example.com"
            val password = "abcdef"

            val signInTask = auth.signInWithEmailAndPassword(email, password)

            // Wait until sign-in Task finishes
            Tasks.await(signInTask)

            // Fail the test setup if sign-in did not succeed
            assertTrue("FirebaseAuth signIn failed in @Before", signInTask.isSuccessful)
        }

        // 4) Initialize Espresso Intents
        Intents.init()

        composeRule.activityRule.scenario.recreate()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    /**
     * Verifies that the MyTeams screen displays:
     *  - Top AppBar title "Teams"
     *  - Bottom navigation items
     */
    @Test
    fun myTeams_topBar_and_bottomNavigation_visible_onLaunch() {
        // Top bar
        composeRule.onNodeWithText("Teams")
            .assertIsDisplayed()

        // Bottom navigation
        composeRule.onNodeWithText("My Preferences")
            .assertIsDisplayed()
        composeRule.onNodeWithText("My Teams")
            .assertIsDisplayed()
        composeRule.onNodeWithText("My Profile")
            .assertIsDisplayed()
    }

    /**
     * Verifies that:
     *  - "Create new team" button opens the dialog
     *  - "Cancel" button closes it
     */
    @Test
    fun myTeams_createTeamDialog_opens_and_closes() {
        // Dialog should not exist initially
        composeRule.onNodeWithText("Create New Team")
            .assertDoesNotExist()

        // Open dialog
        composeRule.onNodeWithText("Create new team")
            .assertIsDisplayed()
            .performClick()

        // Dialog contents should be visible
        composeRule.onNodeWithText("Create New Team")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Team Name")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Create")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Cancel")
            .assertIsDisplayed()

        // Close dialog
        composeRule.onNodeWithText("Cancel")
            .performClick()

        // Dialog should disappear
        composeRule.onNodeWithText("Create New Team")
            .assertDoesNotExist()
    }

    /**
     * Verifies that tapping the "My Profile" button
     * launches ProfileActivity via navigation.
     */
    @Test
    fun myTeams_bottomNavigation_navigatesToProfileActivity() {
        composeRule.onNodeWithText("My Profile")
            .assertIsDisplayed()
            .performClick()

        intended(hasComponent(ProfileActivity::class.java.name))
    }

    /**
     * Verifies that tapping the "My Preferences" button
     * launches MyPreferencesActivity via navigation.
     */
    @Test
    fun myTeams_bottomNavigation_navigatesToMyPreferencesActivity() {
        composeRule.onNodeWithText("My Preferences")
            .assertIsDisplayed()
            .performClick()

        intended(hasComponent(MyPreferencesActivity::class.java.name))
    }

    /**
     * Verifies that "My Teams" tab remains visible even after
     * navigating to other tabs in the bottom navigation bar.
     */
    @Test
    fun myTeams_bottomNavigation_myTeamsTab_isAlwaysVisible() {
        composeRule.onNodeWithText("My Teams")
            .assertIsDisplayed()

        composeRule.onNodeWithText("My Preferences")
            .performClick()
        composeRule.onNodeWithText("My Teams")
            .assertIsDisplayed()

        composeRule.onNodeWithText("My Profile")
            .performClick()
        composeRule.onNodeWithText("My Teams")
            .assertIsDisplayed()
    }

    /**
     * Verifies that the Create Team dialog stays open
     * when "Create" is pressed with an empty team name.
     * This implicitly checks that basic validation is enforced.
     */
    @Test
    fun myTeams_createTeamDialog_staysOpen_whenTeamNameEmpty() {
        // Open dialog
        composeRule.onNodeWithText("Create new team")
            .assertIsDisplayed()
            .performClick()

        // Dialog should be visible
        composeRule.onNodeWithText("Create New Team")
            .assertIsDisplayed()

        // Press "Create" without entering any text
        composeRule.onNodeWithText("Create")
            .assertIsDisplayed()
            .performClick()

        // Dialog should still be visible (validation prevents closing)
        composeRule.onNodeWithText("Create New Team")
            .assertIsDisplayed()
    }

    /**
     * Verifies that the bottom navigation bar remains visible
     * while the Create Team dialog is open.
     */
    @Test
    fun myTeams_bottomNavigation_visible_whileCreateDialogOpen() {
        // Open dialog
        composeRule.onNodeWithText("Create new team")
            .assertIsDisplayed()
            .performClick()

        // Dialog is visible
        composeRule.onNodeWithText("Create New Team")
            .assertIsDisplayed()

        // Bottom navigation items should still be visible behind the dialog
        composeRule.onNodeWithText("My Preferences")
            .assertIsDisplayed()
        composeRule.onNodeWithText("My Teams")
            .assertIsDisplayed()
        composeRule.onNodeWithText("My Profile")
            .assertIsDisplayed()
    }

    /**
     * Verifies that the Create Team dialog can be opened
     * and closed multiple times without crashing or getting stuck.
     */
    @Test
    fun myTeams_createTeamDialog_canBeOpenedMultipleTimes() {
        // 1st open
        composeRule.onNodeWithText("Create new team")
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("Create New Team")
            .assertIsDisplayed()

        // 1st close
        composeRule.onNodeWithText("Cancel")
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("Create New Team")
            .assertDoesNotExist()

        // 2nd open
        composeRule.onNodeWithText("Create new team")
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("Create New Team")
            .assertIsDisplayed()

        // 2nd close
        composeRule.onNodeWithText("Cancel")
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("Create New Team")
            .assertDoesNotExist()
    }

    /**
     * Verifies that tapping a team card on MyTeams navigates
     * to the TeamDetail screen and shows basic information.
     *
     * Precondition: a team card with title "Integration Test Team" is visible.
     */
    @Test
    fun teamDetail_navigateFromMyTeams_and_showBasicInfo() {
        // Wait until the RecyclerView card with the test team name appears
        waitForTeamCardToAppear()

        // Open TeamDetail by tapping the team card (RecyclerView → Espresso)
        onView(withText(testTeamName))
            .perform(click())

        // Now verify TeamDetail using Compose semantics
        // Top bar title
        composeRule.onNodeWithText(testTeamName)
            .assertIsDisplayed()

        // Summary card labels
        composeRule.onNodeWithText("Members")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Last Meal Poll")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Active Poll")
            .assertIsDisplayed()

        // Team members section
        composeRule.onNodeWithText("Team Members")
            .assertIsDisplayed()
    }

    /**
     * Verifies that the "Start New Poll" button is visible
     * on the TeamDetail screen for the leader user.
     */
    @Test
    fun teamDetail_startNewPollButton_visibleForLeader() {
        // Navigate to TeamDetail from the RecyclerView card
        waitForTeamCardToAppear()
        onView(withText(testTeamName))
            .perform(click())

        // Start New Poll button should be visible
        composeRule.onNodeWithText("Start New Poll")
            .assertIsDisplayed()
    }

    /**
     * Verifies that the bottom navigation bar is visible
     * on the TeamDetail screen as well.
     */
    @Test
    fun teamDetail_bottomNavigation_isVisible() {
        // Navigate to TeamDetail from the RecyclerView card
        waitForTeamCardToAppear()
        onView(withText(testTeamName))
            .perform(click())

        // Bottom navigation items should be visible
        composeRule.onNodeWithText("My Preferences")
            .assertIsDisplayed()
        composeRule.onNodeWithText("My Teams")
            .assertIsDisplayed()
        composeRule.onNodeWithText("My Profile")
            .assertIsDisplayed()
    }



    /**
     * Helper: waits until the RecyclerView card with the test team name
     * is visible on screen, using Espresso to query the Android View hierarchy.
     */
    private fun waitForTeamCardToAppear(timeoutMillis: Long = 10_000L) {
        composeRule.waitUntil(timeoutMillis) {
            try {
                onView(withText(testTeamName))
                    .check(matches(isDisplayed()))
                true
            } catch (_: NoMatchingViewException) {
                false
            } catch (_: AssertionFailedError) {
                false
            }
        }
    }

    @Test
    fun teamDetail_backButton_returnsToMyTeamsList() {
        // Navigate to TeamDetail from the RecyclerView card
        waitForTeamCardToAppear()
        onView(withText(testTeamName))
            .perform(click())

        // 2) Verify TeamDetail is shown
        composeRule.onNodeWithText("Team Members")
            .assertIsDisplayed()

        // 3) Press back
        composeRule.onNodeWithContentDescription("Back")
            .assertIsDisplayed()
            .performClick()

        // 4) WAIT until the new MyTeamsActivity Compose tree is fully visible
        composeRule.waitUntil(10_000L) {
            composeRule.onAllNodesWithText("Teams")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // 5) Assertions (now guaranteed to succeed)
        composeRule.onNodeWithText("Teams")
            .assertIsDisplayed()

        composeRule.onNodeWithText("Create new team")
            .assertIsDisplayed()
    }
}
