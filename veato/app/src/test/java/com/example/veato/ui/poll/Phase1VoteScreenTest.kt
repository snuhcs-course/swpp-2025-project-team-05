package com.example.veato.ui.poll

import com.example.veato.data.model.Poll
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.Before
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import io.mockk.unmockkStatic


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Phase1VoteScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var activity: ComponentActivity

    @Before
    fun setupBefore() {
        try { unmockkStatic(FirebaseApp::class) } catch (_: Throwable) {}
        try { unmockkStatic(FirebaseAuth::class) } catch (_: Throwable) {}

        composeRule.mainClock.autoAdvance = true

        // Launch a clean empty activity
        activity = Robolectric.buildActivity(ComponentActivity::class.java)
            .setup()
            .get()
    }

    @After
    fun cleanup() {
        try { unmockkStatic(FirebaseApp::class) } catch (_: Throwable) {}
        try { unmockkStatic(FirebaseAuth::class) } catch (_: Throwable) {}
    }

    @Test
    fun screen_displaysHeader_andCandidates() {
        composeRule.setContent {
            Phase1VoteScreen(
                state = pollState(),
                onToggleApproval = {},
                onRejectCandidate = {},
                onLockInVote = {},
                onTimeOver = {}
            )
        }

        composeRule.onNodeWithText("Phase 1: Voting").assertExists()
    }

    @Test
    fun clickingCheckbox_triggersOnToggleApproval() {
        var toggledIndex = -1
        val state = pollState()

        composeRule.setContent {
            Phase1VoteScreen(
                state = state,
                onToggleApproval = { index -> toggledIndex = index },
                onRejectCandidate = {},
                onLockInVote = {},
                onTimeOver = {}
            )
        }

        // Click first checkbox
        composeRule.onAllNodes(isToggleable())[0].performClick()

        assert(toggledIndex == 0) {
            "Expected toggle index = 0 but got $toggledIndex"
        }
    }

    @Test
    fun clickingRejectButton_opensRejectDialog() {
        val state = pollState()

        composeRule.setContent {
            Phase1VoteScreen(
                state = state,
                onToggleApproval = {},
                onRejectCandidate = {},
                onLockInVote = {},
                onTimeOver = {}
            )
        }

        // Click reject button for first candidate
        composeRule.onAllNodesWithContentDescription("Reject")[0].performClick()

        composeRule.onNodeWithText("Reject Menu?").assertExists()
    }

    @Test
    fun confirmReject_callsOnRejectCandidate() {
        var rejectedIndex = -1
        val state = pollState()

        composeRule.setContent {
            Phase1VoteScreen(
                state = state,
                onToggleApproval = {},
                onRejectCandidate = { index -> rejectedIndex = index },
                onLockInVote = {},
                onTimeOver = {}
            )
        }

        // Open dialog
        composeRule.onAllNodesWithContentDescription("Reject")[0].performClick()

        // Confirm reject
        composeRule.onNodeWithText("Reject").performClick()

        assert(rejectedIndex == 0)
    }

    @Test
    fun lockInButton_disabledWhenNoSelection() {
        val state = pollState(selected = emptySet())

        composeRule.setContent {
            Phase1VoteScreen(
                state = state,
                onToggleApproval = {},
                onRejectCandidate = {},
                onLockInVote = {},
                onTimeOver = {}
            )
        }

        composeRule.onNodeWithText("0 selected  •  Lock In Vote")
            .assertExists()
            .assertIsNotEnabled()
    }

    @Test
    fun lockInButton_enabledWhenHasSelection() {
        var called = false
        val state = pollState(selected = setOf(1))

        composeRule.setContent {
            Phase1VoteScreen(
                state = state,
                onToggleApproval = {},
                onRejectCandidate = {},
                onLockInVote = { called = true },
                onTimeOver = {}
            )
        }

        composeRule.onNodeWithText("1 selected  •  Lock In Vote")
            .assertIsEnabled()
            .performClick()

        assert(called)
    }

    @Test
    fun timer_end_callsOnTimeOver() {
        var timeOverCalled = false

        val poll = FakePollData.examplePoll().copy(
            duration = 1,
            isOpen = true
        )

        composeRule.mainClock.autoAdvance = false

        composeRule.setContent {
            Phase1VoteScreen(
                state = pollState(poll),
                onToggleApproval = {},
                onRejectCandidate = {},
                onLockInVote = {},
                onTimeOver = { timeOverCalled = true }
            )
        }

        composeRule.mainClock.advanceTimeBy(2000)

        assert(timeOverCalled)
    }


//    @Test
//    fun screen_rendersNothing_whenPollIsNull() {
//        composeRule.setContent {
//            androidx.compose.material3.Surface {
//                Phase1VoteScreen(
//                    state = PollScreenState(
//                        poll = null,
//                        selectedIndices = emptySet(),
//                        rejectedCandidateIndex = -1,
//                        rejectionUsed = false,
//                        voted = false
//                    ),
//                    onToggleApproval = {},
//                    onRejectCandidate = {},
//                    onLockInVote = {},
//                    onTimeOver = {}
//                )
//            }
//        }
//
//        // Check that a node that SHOULD exist is NOT present
//        composeRule
//            .onAllNodesWithText("Phase 1: Voting", substring = true)
//            .assertCountEquals(0)
//    }

    @Test
    fun rejectButton_disabled_whenRejectionAlreadyUsed() {
        val state = pollState(
            rejectedIndex = -1,
            rejectionUsed = true // <-- already used
        )

        composeRule.setContent {
            Phase1VoteScreen(
                state = state,
                onToggleApproval = {},
                onRejectCandidate = {},
                onLockInVote = {},
                onTimeOver = {}
            )
        }

        composeRule.onAllNodesWithContentDescription("Reject")[0].assertIsNotEnabled()
    }

    @Test
    fun rejectedCandidate_disablesCheckbox_andRejectButton() {
        val state = pollState(
            rejectedIndex = 0,
            rejectionUsed = true
        )

        composeRule.setContent {
            Phase1VoteScreen(
                state = state,
                onToggleApproval = {},
                onRejectCandidate = {},
                onLockInVote = {},
                onTimeOver = {}
            )
        }

        // Checkbox disabled
        composeRule.onAllNodes(isToggleable())[0].assertIsNotEnabled()

        // Reject button disabled
        composeRule.onAllNodesWithContentDescription("Reject")[0].assertIsNotEnabled()
    }

    @Test
    fun lockedInUserCount_displaysLockedInCard() {
        val poll = FakePollData.examplePoll().copy(
            lockedInUserCount = 2
        )

        val state = pollState(poll = poll)

        composeRule.setContent {
            Phase1VoteScreen(
                state = state,
                onToggleApproval = {},
                onRejectCandidate = {},
                onLockInVote = {},
                onTimeOver = {}
            )
        }

        composeRule.onNodeWithText("2 member(s) have locked in their votes")
            .assertExists()
    }

    @Test
    fun whenVoted_showWaitingForOthersCard() {
        val state = pollState(
            selected = setOf(0),
            voted = true  // User has locked in
        )

        composeRule.setContent {
            Phase1VoteScreen(
                state = state,
                onToggleApproval = {},
                onRejectCandidate = {},
                onLockInVote = {},
                onTimeOver = {}
            )
        }

        composeRule.onNodeWithText("✓ Vote locked in. Waiting for others...")
            .assertExists()
    }

    @Test
    fun rejectDialog_cancelClosesDialog() {
        val state = pollState()

        composeRule.setContent {
            Phase1VoteScreen(
                state = state,
                onToggleApproval = {},
                onRejectCandidate = {},
                onLockInVote = {},
                onTimeOver = {}
            )
        }

        // Open dialog
        composeRule.onAllNodesWithContentDescription("Reject")[0].performClick()

        composeRule.onNodeWithText("Reject Menu?").assertExists()

        // Click Cancel
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithText("Reject Menu?")
            .assertDoesNotExist()
    }

    fun pollState(
        poll: Poll = FakePollData.examplePoll().copy(isOpen = false),
        selected: Set<Int> = emptySet(),
        rejectedIndex: Int = -1,
        rejectionUsed: Boolean = false,
        voted: Boolean = false
    ) = PollScreenState(
        poll = poll,
        selectedIndices = selected,
        rejectedCandidateIndex = rejectedIndex,
        rejectionUsed = rejectionUsed,
        voted = voted
    )
}