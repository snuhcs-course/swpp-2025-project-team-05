package com.example.veato

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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
import org.junit.*
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith

/**
 * UI Integration Test for VoteSettingActivity.
 *
 * Flow tested:
 *  MyTeams → TeamDetail → VoteSettingActivity
 *
 * This test opens the actual flow, passes real Intent extras,
 * and then validates the VoteSetting UI.
 */
@RunWith(AndroidJUnit4::class)
@MediumTest
class VoteSettingActivityIntegrationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MyTeamsActivity>()

    private lateinit var auth: FirebaseAuth

    // 실제 MyTeams 화면에 존재하는 테스트 팀 이름
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

        // Recreate the Activity after login
        composeRule.activityRule.scenario.recreate()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    /**
     * Full flow:
     *  MyTeams → TeamDetail → Start New Poll → VoteSettingActivity
     * Afterwards, verify VoteSetting UI elements.
     */
    @Test
    fun voteSetting_fullFlow_basicUIVisible() {
        // 1) MyTeams에서 testTeamName 카드가 로딩될 때까지 기다림
        waitForTeamCardToAppear()

        // 2) RecyclerView → TeamDetail 이동
        onView(withText(testTeamName))
            .perform(click())

        // 3) TeamDetail 화면에서 "Start New Poll" 버튼 클릭
        composeRule.onNodeWithText("Start New Poll")
            .assertIsDisplayed()
            .performClick()

        // 4) VoteSettingActivity로 인텐트 이동 확인
        intended(hasComponent(VoteSettingActivity::class.java.name))

        // 5) VoteSetting UI 요소들 검증
        composeRule.onNodeWithText("New Poll Settings")
            .assertIsDisplayed()

        composeRule.onNodeWithText("$testTeamName - New Poll Settings")
            .assertIsDisplayed()

        composeRule.onNodeWithText("Poll Title")
            .assertIsDisplayed()

        composeRule.onNodeWithText("Poll Duration")
            .assertIsDisplayed()

        composeRule.onNodeWithText("Poll Members")
            .assertIsDisplayed()

        composeRule.onNodeWithText("Poll Occasion (optional)")
            .assertIsDisplayed()

        composeRule.onNodeWithText("Start New Poll")
            .assertIsDisplayed()
    }

    /**
     * Duration dropdown interaction test
     */
    @Test
    fun voteSetting_fullFlow_durationDropdown_worksCorrectly() {
        // 1) MyTeams → TeamDetail → VoteSetting 이동
        waitForTeamCardToAppear()
        onView(withText(testTeamName)).perform(click())
        composeRule.onNodeWithText("Start New Poll").performClick()

        intended(hasComponent(VoteSettingActivity::class.java.name))

        // 2) default: 3 minutes
        composeRule.onNodeWithText("3 minutes")
            .assertIsDisplayed()
            .performClick()

        // 3) pick 5
        composeRule.onNodeWithText("5 minutes")
            .assertIsDisplayed()
            .performClick()

        // 4) updated
        composeRule.onNodeWithText("5 minutes")
            .assertIsDisplayed()
    }

    /**
     * Validation test — try Start without title
     */
    @Test
    fun voteSetting_fullFlow_emptyTitle_doesNotCrash() {
        // 1) MyTeams → TeamDetail → VoteSetting 이동
        waitForTeamCardToAppear()
        onView(withText(testTeamName)).perform(click())
        composeRule.onNodeWithText("Start New Poll").performClick()

        // 2) Title 비운 채 Start
        composeRule.onNodeWithText("Start New Poll")
            .performClick()

        // 3) Crash 없이 그대로 UI 존재해야 함
        composeRule.onNodeWithText("New Poll Settings")
            .assertIsDisplayed()

        composeRule.onNodeWithText("Poll Title")
            .assertIsDisplayed()
    }

    /**
     * Title / Occasion text fields can be edited.
     * (No poll start; we only type text and verify it's shown.)
     */
    @Test
    fun voteSetting_fullFlow_canEditTitleAndOccasion() {
        // 1) Flow: MyTeams → TeamDetail → VoteSetting
        waitForTeamCardToAppear()
        onView(withText(testTeamName)).perform(click())
        composeRule.onNodeWithText("Start New Poll").performClick()

        // 2) Type into Poll Title
        composeRule.onNodeWithText("e.g. 10/25 team dinner")
            .assertIsDisplayed()
            .performTextInput("Team dinner after meeting")

        // 입력한 텍스트가 화면에 표시되는지 확인
        composeRule.onNodeWithText("Team dinner after meeting")
            .assertIsDisplayed()

        // 3) Type into Occasion note
        composeRule.onNodeWithText("e.g. We have kids with us tonight, prefer milder options")
            .assertIsDisplayed()
            .performTextInput("We have kids, please pick mild options")

        composeRule.onNodeWithText("We have kids, please pick mild options")
            .assertIsDisplayed()
    }

    /**
     * Unselect all members and press Start:
     * validation should prevent poll start, and screen stays visible.
     * (We give a title so that only "no members" validation triggers.)
     */
    @Test
    fun voteSetting_fullFlow_unselectAllMembers_validationPreventsPollStart() {
        // 1) Flow: MyTeams → TeamDetail → VoteSetting
        waitForTeamCardToAppear()
        onView(withText(testTeamName)).perform(click())
        composeRule.onNodeWithText("Start New Poll").performClick()

        // 2) Unselect all checkboxes (members)
        val checkboxes = composeRule.onAllNodes(isToggleable())
        val size = checkboxes.fetchSemanticsNodes().size
        for (i in 0 until size) {
            checkboxes[i].performClick()
        }

        // 3) Fill a non-empty title so title validation passes
        composeRule.onNodeWithText("e.g. 10/25 team dinner")
            .performTextInput("Test poll")

        // 4) Press Start New Poll.
        //    Because selectedMembers is empty, validation will block poll start.
        composeRule.onNodeWithText("Start New Poll")
            .performClick()

        // 5) Still on VoteSetting screen → basic UI remains visible
        composeRule.onNodeWithText("New Poll Settings")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Poll Members")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Start New Poll")
            .assertIsDisplayed()
    }

    /**
     * After navigating to VoteSetting, all team members
     * should be selected by default.
     */
    @Test
    fun voteSetting_fullFlow_allMembersSelectedByDefault() {
        // MyTeams → TeamDetail → VoteSetting
        waitForTeamCardToAppear()
        onView(withText(testTeamName)).perform(click())
        composeRule.onNodeWithText("Start New Poll").performClick()

        // Wait until member checkboxes are loaded
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(isToggleable())
                .fetchSemanticsNodes().isNotEmpty()
        }

        val total = composeRule.onAllNodes(isToggleable())
            .fetchSemanticsNodes().size
        val checked = composeRule.onAllNodes(isToggleable() and isOn())
            .fetchSemanticsNodes().size

        check(checked == total) {
            "Expected all members to be selected by default, but $checked of $total are checked."
        }
    }

    /**
     * Toggling a single member checkbox should update
     * the selection count (without starting a poll).
     */
    @Test
    fun voteSetting_fullFlow_togglingSingleMember_updatesSelectionState() {
        // MyTeams → TeamDetail → VoteSetting
        waitForTeamCardToAppear()
        onView(withText(testTeamName)).perform(click())
        composeRule.onNodeWithText("Start New Poll").performClick()

        // Wait until member checkboxes are loaded
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(isToggleable())
                .fetchSemanticsNodes().isNotEmpty()
        }

        val allToggleables = composeRule.onAllNodes(isToggleable())

        val initialChecked = composeRule.onAllNodes(isToggleable() and isOn())
            .fetchSemanticsNodes().size

        // Toggle first checkbox
        allToggleables[0].performClick()

        val afterChecked = composeRule.onAllNodes(isToggleable() and isOn())
            .fetchSemanticsNodes().size

        check(afterChecked == initialChecked - 1) {
            "Expected one member to be deselected, but checked count changed from $initialChecked to $afterChecked."
        }
    }

    /**
     * A title containing only whitespace should be treated as blank
     * and rejected by validation (no poll start).
     */
    @Test
    fun voteSetting_fullFlow_whitespaceOnlyTitle_isRejected() {
        // MyTeams → TeamDetail → VoteSetting
        waitForTeamCardToAppear()
        onView(withText(testTeamName)).perform(click())
        composeRule.onNodeWithText("Start New Poll").performClick()

        // Type only spaces into title field
        composeRule.onNodeWithText("e.g. 10/25 team dinner")
            .performTextInput("   ")

        // Press Start
        composeRule.onNodeWithText("Start New Poll")
            .performClick()

        // Still on VoteSetting screen → validation prevented poll start
        composeRule.onNodeWithText("New Poll Settings")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Poll Title")
            .assertIsDisplayed()
    }








    // -------------------------
    // Helper: Wait for Team card
    // -------------------------
    private fun waitForTeamCardToAppear(timeoutMillis: Long = 10000L) {
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
}
