package com.example.veato

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import org.junit.Assert.assertTrue

/**
 * UI integration tests for ProfileActivity + ProfileScreen.
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class ProfileActivityIntegrationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ProfileActivity>()
    private lateinit var auth: FirebaseAuth

    @Before
    fun setUp() {
        // 1) Firebase 초기화
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext())

        // 2) Auth 인스턴스 가져오기
        auth = FirebaseAuth.getInstance()

        // 3) 이미 로그인되어 있지 않다면, 테스트용 계정으로 로그인
        if (auth.currentUser == null) {
            val email = "abcdef@example.com"
            val password = "abcdef"

            val signInTask = auth.signInWithEmailAndPassword(email, password)

            // 비동기 Task 완료까지 기다리기
            Tasks.await(signInTask)

            // 로그인 성공 여부 확인 (실패하면 테스트 자체가 깨지도록)
            assertTrue("FirebaseAuth signIn failed in @Before", signInTask.isSuccessful)
        }

        // 4) Espresso Intents 초기화
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    /**
     * BottomNavigation: tapping "My Preferences" should open MyPreferencesActivity.
     */
    @Test
    fun clickingMyPreferences_startsMyPreferencesActivity() {
        composeRule.onNodeWithText("My Preferences")
            .assertIsDisplayed()
            .performClick()

        intended(hasComponent(MyPreferencesActivity::class.java.name))
    }

    /**
     * BottomNavigation: tapping "My Teams" should open MyTeamsActivity.
     */
    @Test
    fun clickingMyTeams_startsMyTeamsActivity() {
        composeRule.onNodeWithText("My Teams")
            .assertIsDisplayed()
            .performClick()

        intended(hasComponent(MyTeamsActivity::class.java.name))
    }

    /**
     * BottomNavigation: tapping "My Profile" on the current screen
     * should not crash the app.
     */
    @Test
    fun clickingMyProfile_doesNotCrash() {
        composeRule.onNodeWithText("My Profile")
            .assertIsDisplayed()
            .performClick()
        // If the test finishes without an exception, this passes.
    }

    /**
     * BottomNavigation: all three items should be visible.
     */
    @Test
    fun bottomNavigation_existsAndShowsAllItems() {
        composeRule.onNodeWithText("My Preferences")
            .assertIsDisplayed()
        composeRule.onNodeWithText("My Teams")
            .assertIsDisplayed()
        composeRule.onNodeWithText("My Profile")
            .assertIsDisplayed()
    }

    /**
     * TopAppBar: back button should exist and be clickable.
     */
    @Test
    fun backButton_clickable() {
        composeRule.onNodeWithContentDescription("Back")
            .assertIsDisplayed()
            .performClick()
    }

    /**
     * Initial state: TopAppBar title should be "Profile" in view mode.
     */
    @Test
    fun topAppBar_showsProfileTitleInViewMode() {
        composeRule.onNodeWithText("Profile")
            .assertIsDisplayed()
    }

    /**
     * Initial state: edit-mode actions ("Cancel", "Save") should not be visible
     * because isEditing is false by default.
     */
    @Test
    fun editModeActions_notVisibleInViewMode() {
        composeRule.onNodeWithContentDescription("Cancel")
            .assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Save")
            .assertDoesNotExist()
    }

    /**
     * TopAppBar: there should be exactly one back button.
     */
    @Test
    fun backButton_onlyOneInstance() {
        composeRule.onAllNodesWithContentDescription("Back")
            .assertCountEquals(1)
    }

    /**
     * BottomNavigation: each item text should exist exactly once.
     */
    @Test
    fun bottomNavigation_itemsAreUnique() {
        composeRule.onAllNodesWithText("My Preferences")
            .assertCountEquals(1)
        composeRule.onAllNodesWithText("My Teams")
            .assertCountEquals(1)
        composeRule.onAllNodesWithText("My Profile")
            .assertCountEquals(1)
    }

    /**
     * Edit mode entry test:
     * - Tapping the "Edit Profile" button enters edit mode.
     * - TopAppBar title changes to "Edit Profile".
     * - "Cancel" and "Save" icons become visible.
     * - The original "Edit Profile" button disappears.
     */
    @Test
    fun clickingEditProfile_entersEditModeAndShowsEditAppBar() {
        val editButtonMatcher = hasText("Edit Profile") and hasClickAction()

        composeRule.onNode(editButtonMatcher)
            .assertIsDisplayed()
            .performClick()

        // Edit mode TopAppBar
        composeRule.onNodeWithText("Edit Profile")
            .assertIsDisplayed()

        // Edit mode actions
        composeRule.onNodeWithContentDescription("Cancel")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Save")
            .assertIsDisplayed()

        // Edit button should no longer be visible as a clickable item
        composeRule.onNode(editButtonMatcher)
            .assertDoesNotExist()
    }

    /**
     * Cancel behavior in edit mode:
     * - Tapping "Cancel" returns to view mode.
     * - TopAppBar title reverts to "Profile".
     * - "Cancel" and "Save" icons disappear.
     * - The "Edit Profile" button becomes visible again.
     */
    @Test
    fun cancelFromEditMode_returnsToViewMode() {
        val editButtonMatcher = hasText("Edit Profile") and hasClickAction()

        // Enter edit mode
        composeRule.onNode(editButtonMatcher)
            .assertIsDisplayed()
            .performClick()

        // Tap Cancel
        composeRule.onNodeWithContentDescription("Cancel")
            .assertIsDisplayed()
            .performClick()

        // Back to view mode
        composeRule.onNodeWithText("Profile")
            .assertIsDisplayed()

        // Edit actions should disappear
        composeRule.onNodeWithContentDescription("Cancel")
            .assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Save")
            .assertDoesNotExist()

        // "Edit Profile" button should be visible again
        composeRule.onNode(editButtonMatcher)
            .assertIsDisplayed()
    }

    /**
     * BottomNavigationBar presence in edit mode:
     */
    @Test
    fun bottomNavigation_isPresentInEditMode() {
        val editButtonMatcher = hasText("Edit Profile") and hasClickAction()

        // Enter edit mode
        composeRule.onNode(editButtonMatcher)
            .assertExists()
            .performClick()

        // BottomNavigationBar items should exist in the semantics tree,
        // even if not considered "displayed" by the test framework.
        composeRule.onNodeWithText("My Preferences").assertExists()
        composeRule.onNodeWithText("My Teams").assertExists()
        composeRule.onNodeWithText("My Profile").assertExists()
    }


    /**
     * Dietary Restrictions section shows expected chips.
     */
    @Test
    fun dietaryRestrictions_chipsAreVisible() {
        composeRule.onNodeWithText("Dietary Restrictions")
            .assertIsDisplayed()

        composeRule.onNodeWithText("Vegan")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Halal")
            .assertIsDisplayed()
    }




    /**
     * Profile picture is clickable in Edit mode.
     */
    @Test
    fun profilePicture_isClickableInEditMode() {
        composeRule.onNodeWithText("Edit Profile")
            .performClick()

        composeRule.onNodeWithContentDescription("Change picture")
            .assertIsDisplayed()
            .performClick()
    }


    /**
     * Save button exits edit mode.
     */
    @Test
    fun saveProfile_exitsEditMode() {
        composeRule.onNodeWithText("Edit Profile")
            .performClick()

        composeRule.onNodeWithContentDescription("Save")
            .assertIsDisplayed()
            .performClick()

        composeRule.onNodeWithText("Edit Profile")
            .assertIsDisplayed()
    }



    /**
     * Edit mode → Cancel → returns to view mode.
     */
    @Test
    fun editMode_cancel_returnsToViewMode() {
        val editButtonMatcher = hasText("Edit Profile") and hasClickAction()

        // Enter edit mode
        composeRule.onNode(editButtonMatcher)
            .assertIsDisplayed()
            .performClick()

        // Tap Cancel (X)
        composeRule.onNodeWithContentDescription("Cancel")
            .assertIsDisplayed()
            .performClick()

        // Back to view mode
        composeRule.onNode(editButtonMatcher)
            .assertIsDisplayed()
    }


}
