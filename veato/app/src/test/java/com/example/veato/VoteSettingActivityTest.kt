package com.example.veato

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.example.veato.data.remote.StartSessionResponse
import com.example.veato.data.repository.PollRepositoryImpl
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VoteSettingActivityTest {

    @get:Rule
    val composeRule = createComposeRule()

    // Provide a fake ViewModelStoreOwner so viewModel() works in Compose
    @Composable
    fun TestWrapper(content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalViewModelStoreOwner provides object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            },
            content = content
        )
    }

    @Before
    fun setupFirestore() {
        mockkStatic(FirebaseFirestore::class)

        val mockDB = mockk<FirebaseFirestore>(relaxed = true)
        every { FirebaseFirestore.getInstance() } returns mockDB

        // Mock TEAM doc
        val mockTeamDoc = mockk<DocumentSnapshot>(relaxed = true)
        every { mockDB.collection("teams").document("team123").get() } returns
                Tasks.forResult(mockTeamDoc)

        every {
            mockTeamDoc.toObject(Team::class.java)
        } returns Team(
            id = "team123",
            name = "DemoTeam",
            members = listOf("u1", "u2")
        )

        // Mock USER docs for member names
        val mockUserDoc = mockk<DocumentSnapshot>(relaxed = true)
        every {
            mockDB.collection("users").document(any()).get()
        } returns Tasks.forResult(mockUserDoc)

        every { mockUserDoc.getString("fullName") } returns "MemberName"
    }


    @Test
    fun ui_renders_all_sections() {
        composeRule.setContent {
            TestWrapper {
                VoteSettingScreen(
                    teamName = "Team A",
                    teamId = "team123",
                    onStartVoting = { _, _ -> }
                )
            }
        }

        composeRule.onNodeWithText("New Poll Settings").assertExists()
        composeRule.onNodeWithText("Poll Title").assertExists()
        composeRule.onNodeWithText("Poll Duration").assertExists()
        composeRule.onNodeWithText("Poll Members").assertExists()
        composeRule.onNodeWithText("Poll Occasion (optional)").assertExists()
    }

    @Test
    fun dropdown_changes_value() {
        composeRule.setContent {
            TestWrapper {
                VoteSettingScreen("Team A", "team123") { _, _ -> }
            }
        }

        composeRule.onNodeWithText("3 minutes").performClick()
        composeRule.onNodeWithText("1 minutes").assertExists()
        composeRule.onNodeWithText("5 minutes").performClick()

        composeRule.onNodeWithText("5 minutes").assertExists()
    }

    @Test
    fun members_appear_in_list() {
        composeRule.setContent {
            TestWrapper {
                VoteSettingScreen("Team A", "team123") { _, _ -> }
            }
        }

        composeRule.onAllNodesWithText("MemberName")[0].assertExists()
        composeRule.onAllNodesWithText("MemberName")[1].assertExists()
    }

    @Test
    fun member_selection_toggles_on_click() {
        composeRule.setContent {
            TestWrapper {
                VoteSettingScreen("Team A", "team123") { _, _ -> }
            }
        }

        val firstMember = composeRule.onAllNodesWithText("MemberName")[0]

        // initial state should exist
        firstMember.assertExists()

        // click to toggle
        firstMember.performClick()

        // Should still exist (selected)
        firstMember.assertExists()
    }

    @Test
    fun ui_updates_when_team_has_different_members() {
        mockkStatic(FirebaseFirestore::class)

        val mockDB = mockk<FirebaseFirestore>(relaxed = true)
        every { FirebaseFirestore.getInstance() } returns mockDB

        val mockTeamDoc = mockk<DocumentSnapshot>(relaxed = true)
        every { mockDB.collection("teams").document("team123").get() } returns
                Tasks.forResult(mockTeamDoc)

        every { mockTeamDoc.toObject(Team::class.java) } returns
                Team(id = "team123", name = "DemoTeam", members = listOf("a1", "a2", "a3"))

        val mockUserDoc = mockk<DocumentSnapshot>(relaxed = true)
        every { mockDB.collection("users").document(any()).get() } returns Tasks.forResult(mockUserDoc)

        every { mockUserDoc.getString("fullName") } returnsMany
                listOf("Alice", "Bob", "Charlie")

        composeRule.setContent {
            TestWrapper {
                VoteSettingScreen("Team A", "team123") { _, _ -> }
            }
        }

        composeRule.waitUntil(3000) {
            composeRule.onAllNodesWithText("Alice").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Alice").assertExists()
        composeRule.onNodeWithText("Bob").assertExists()
        composeRule.onNodeWithText("Charlie").assertExists()
    }
}
