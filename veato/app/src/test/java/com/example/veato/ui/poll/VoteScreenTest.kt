package com.example.veato.ui.poll

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.veato.data.model.Candidate
import com.example.veato.data.model.Poll
import com.example.veato.ui.theme.VeatoTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VoteScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun fakeState(
        voted: Boolean = false,
        selected: Set<Int> = emptySet(),
        isOpen: Boolean = true,
        duration: Int = 60
    ): PollScreenState {
        return PollScreenState(
            poll = Poll(
                teamName = "Team Foodies",
                pollTitle = "Lunch Vote",
                duration = duration,
                isOpen = isOpen,
                hasCurrentUserLockedIn = voted,
                candidates = listOf(
                    Candidate("Korean BBQ", 10),
                    Candidate("Ramen", 7),
                    Candidate("Fried Rice", 4),
                    Candidate("Bibimbap", 3),
                    Candidate("Udon", 2)
                )
            ),
            selectedIndices = selected,
            voted = voted
        )
    }

    @Test
    fun screen_renders_header_and_candidates() {
        composeRule.setContent {
            VeatoTheme {
                VoteScreen(
                    state = fakeState(),
                    onSelect = {},
                    onVote = {},
                    onCancel = {},
                    onTimeOver = {}
                )
            }
        }

        composeRule.onNodeWithText("Team Foodies").assertExists()
        composeRule.onNodeWithText("Lunch Vote").assertExists()

        composeRule.onNodeWithText("Korean BBQ").assertExists()
        composeRule.onNodeWithText("Ramen").assertExists()
        composeRule.onNodeWithText("Fried Rice").assertExists()
        composeRule.onNodeWithText("Bibimbap").assertExists()
        composeRule.onNodeWithText("Udon").assertExists()
    }

    @Test
    fun clickingCandidate_triggers_onSelect_withCorrectIndex() {
        var selectedIndex: Int? = null

        composeRule.setContent {
            VeatoTheme {
                VoteScreen(
                    state = fakeState(),
                    onSelect = { selectedIndex = it },
                    onVote = {},
                    onCancel = {},
                    onTimeOver = {}
                )
            }
        }

        composeRule.onNodeWithText("Ramen").performClick()
        assertEquals(1, selectedIndex)
    }

    @Test
    fun voteButton_disabled_when_noSelection() {
        composeRule.setContent {
            VeatoTheme {
                VoteScreen(
                    state = fakeState(selected = emptySet()),
                    onSelect = {},
                    onVote = {},
                    onCancel = {},
                    onTimeOver = {}
                )
            }
        }

        composeRule.onNodeWithText("0 selected  •  Vote")
            .assertExists()
            .assertIsNotEnabled()
    }

    @Test
    fun voteButton_enabled_when_selected() {
        composeRule.setContent {
            VeatoTheme {
                VoteScreen(
                    state = fakeState(selected = setOf(1)),
                    onSelect = {},
                    onVote = {},
                    onCancel = {},
                    onTimeOver = {}
                )
            }
        }

        composeRule.onNodeWithText("1 selected  •  Vote")
            .assertExists()
            .assertIsEnabled()
    }

    @Test
    fun voted_true_shows_cancelVote_button() {
        composeRule.setContent {
            VeatoTheme {
                VoteScreen(
                    state = fakeState(voted = true, selected = setOf(2)),
                    onSelect = {},
                    onVote = {},
                    onCancel = {},
                    onTimeOver = {}
                )
            }
        }

        composeRule.onNodeWithText("Cancel Vote").assertExists()
    }
    
    @Test
    fun when_voted_only_selected_candidate_remains_enabled() {
        composeRule.setContent {
            VeatoTheme {
                VoteScreen(
                    state = fakeState(voted = true, selected = setOf(1)),
                    onSelect = {},
                    onVote = {},
                    onCancel = {},
                    onTimeOver = {}
                )
            }
        }

        composeRule.onNodeWithText("Ramen")   // selected
            .assertIsEnabled()

        composeRule.onNodeWithText("Korean BBQ").assertIsNotEnabled()
        composeRule.onNodeWithText("Fried Rice").assertIsNotEnabled()
        composeRule.onNodeWithText("Bibimbap").assertIsNotEnabled()
        composeRule.onNodeWithText("Udon").assertIsNotEnabled()
    }
}
