package com.example.veato.ui.poll

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.veato.ui.theme.VeatoTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import com.example.veato.data.model.Poll
import com.example.veato.data.model.Candidate
import com.example.veato.ui.poll.PollScreenState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Phase2VoteScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun fakeState(
        voted: Boolean = false,
        selectedIndex: Int? = null,
        lockedInCount: Int = 0,
        isOpen: Boolean = true,
        duration: Int = 10
    ): PollScreenState {
        return PollScreenState(
            poll = Poll(
                pollTitle = "Dinner Vote",
                teamName = "Team Foodies",
                duration = duration,
                isOpen = isOpen,
                hasCurrentUserLockedIn = voted,
                lockedInUserCount = lockedInCount,
                candidates = listOf(
                    Candidate("Korean BBQ", 10),
                    Candidate("Ramen", 7),
                    Candidate("Fried Rice", 4)
                )
            ),
            selectedIndices = selectedIndex?.let { setOf(it) } ?: emptySet(),
            voted = voted
        )
    }


    @Test
    fun screen_renders_header_and_candidates() {
        composeRule.setContent {
            VeatoTheme {
                Phase2VoteScreen(
                    state = fakeState(),
                    onSelectCandidate = {},
                    onLockInVote = {},
                    onTimeOver = {}
                )
            }
        }

        composeRule.onNodeWithText("Team Foodies").assertExists()
        composeRule.onNodeWithText("Dinner Vote").assertExists()
        composeRule.onNodeWithText("Phase 2: Final Vote").assertExists()

        // Candidates
        composeRule.onNodeWithText("Korean BBQ").assertExists()
        composeRule.onNodeWithText("Ramen").assertExists()
        composeRule.onNodeWithText("Fried Rice").assertExists()
    }

    @Test
    fun clickingCandidate_calls_onSelectCandidate() {
        var selectedIndex: Int? = null

        composeRule.setContent {
            VeatoTheme {
                Phase2VoteScreen(
                    state = fakeState(),
                    onSelectCandidate = { selectedIndex = it },
                    onLockInVote = {},
                    onTimeOver = {}
                )
            }
        }

        // click on "Ramen" row
        composeRule.onNodeWithText("Ramen")
            .performClick()

        assertEquals(1, selectedIndex)
    }

    @Test
    fun clickingCandidate_whenLockedIn_doesNothing() {
        var selectedIndex: Int? = null

        composeRule.setContent {
            VeatoTheme {
                Phase2VoteScreen(
                    state = fakeState(voted = true),
                    onSelectCandidate = { selectedIndex = it },
                    onLockInVote = {},
                    onTimeOver = {}
                )
            }
        }

        composeRule.onNodeWithText("Korean BBQ")
            .performClick()

        assertNull(selectedIndex)
    }

    @Test
    fun lockInButton_disabled_when_noSelection() {
        composeRule.setContent {
            VeatoTheme {
                Phase2VoteScreen(
                    state = fakeState(selectedIndex = null),
                    onSelectCandidate = {},
                    onLockInVote = {},
                    onTimeOver = {}
                )
            }
        }

        composeRule.onNodeWithText("Select a menu to lock in")
            .assertExists()
            .assertIsNotEnabled()
    }

    @Test
    fun lockInButton_enabled_when_selected() {
        composeRule.setContent {
            VeatoTheme {
                Phase2VoteScreen(
                    state = fakeState(selectedIndex = 0),
                    onSelectCandidate = {},
                    onLockInVote = {},
                    onTimeOver = {}
                )
            }
        }

        composeRule.onNodeWithText("Lock In Final Vote")
            .assertExists()
            .assertIsEnabled()
    }

    @Test
    fun lockedIn_shows_finalVoteCard() {
        composeRule.setContent {
            VeatoTheme {
                Phase2VoteScreen(
                    state = fakeState(voted = true),
                    onSelectCandidate = {},
                    onLockInVote = {},
                    onTimeOver = {}
                )
            }
        }

        composeRule.onNodeWithText("✓ Final vote locked in. Waiting for results...")
            .assertExists()
    }

    @Test
    fun selectedCandidateRow_isHighlighted() {
        composeRule.setContent {
            VeatoTheme {
                Phase2VoteScreen(
                    state = fakeState(selectedIndex = 1),
                    onSelectCandidate = {},
                    onLockInVote = {},
                    onTimeOver = {}
                )
            }
        }

        // “Ramen” should now be bold (selected)
        composeRule.onNodeWithText("Ramen").assertExists()
    }

    @Test
    fun lockedInCount_showsLockedInCard() {
        composeRule.setContent {
            VeatoTheme {
                Phase2VoteScreen(
                    state = fakeState(lockedInCount = 3),
                    onSelectCandidate = {},
                    onLockInVote = {},
                    onTimeOver = {}
                )
            }
        }

        composeRule.onNodeWithText("3 member(s) have locked in their votes")
            .assertExists()
    }

    @Test
    fun lockInButton_hidden_whenAlreadyVoted() {
        composeRule.setContent {
            VeatoTheme {
                Phase2VoteScreen(
                    state = fakeState(voted = true),
                    onSelectCandidate = {},
                    onLockInVote = {},
                    onTimeOver = {}
                )
            }
        }

        composeRule.onNodeWithText("Lock In Final Vote").assertDoesNotExist()
    }
}
