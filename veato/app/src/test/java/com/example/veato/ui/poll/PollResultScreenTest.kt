package com.example.veato.ui.poll

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.veato.data.model.Candidate
import com.example.veato.data.model.Poll
import com.example.veato.ui.theme.VeatoTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PollResultScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun fakeState(): PollScreenState {
        return PollScreenState(
            poll = Poll(
                pollTitle = "Dinner Vote",
                teamName = "Team Foodies",
                duration = 0,
                isOpen = false,
                hasCurrentUserLockedIn = true,
                lockedInUserCount = 5,
                candidates = emptyList(),
                results = listOf(
                    Candidate("Korean BBQ", 12),
                    Candidate("Ramen", 7),
                    Candidate("Fried Rice", 3)
                )
            ),
            selectedIndices = emptySet(),
            voted = true
        )
    }

    
    @Test
    fun header_renders_correctly() {
        composeRule.setContent {
            VeatoTheme {
                PollResultScreen(
                    state = fakeState(),
                    onBackToMain = {}
                )
            }
        }

        composeRule.onNodeWithText("Team Foodies").assertExists()
        composeRule.onNodeWithText("Dinner Vote").assertExists()
        composeRule.onNodeWithText("Closed").assertExists()
    }

    @Test
    fun winnerRow_displaysTrophyBadge() {
        composeRule.setContent {
            VeatoTheme {
                PollResultScreen(
                    state = fakeState(),
                    onBackToMain = {}
                )
            }
        }

        composeRule.onNodeWithText("🏆").assertExists()
    }

    @Test
    fun emptyResult_noWinnerCard_shown() {
        val emptyState = fakeState().copy(
            poll = fakeState().poll!!.copy(results = emptyList())
        )

        composeRule.setContent {
            VeatoTheme {
                PollResultScreen(
                    state = emptyState,
                    onBackToMain = {}
                )
            }
        }

        composeRule.onNodeWithText("🏆 Winner 🏆")
            .assertDoesNotExist()
    }
}
