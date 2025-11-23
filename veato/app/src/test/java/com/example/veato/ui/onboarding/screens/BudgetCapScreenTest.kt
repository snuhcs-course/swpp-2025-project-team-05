package com.example.veato.ui.onboarding.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.veato.ui.theme.VeatoTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BudgetCapScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var activity: ComponentActivity

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(ComponentActivity::class.java)
            .setup()
            .get()
    }

    @After
    fun cleanup() {}


    @Test
    fun screen_displays_static_texts() {
        composeRule.setContent {
            VeatoTheme {
                BudgetCapScreen(
                    budgetCap = null,
                    onUpdate = {},
                    currentStep = 1,
                    totalSteps = 5,
                    onNext = {},
                    onPrevious = {}
                )
            }
        }

        composeRule.onNodeWithText("Budget Cap").assertExists()
        composeRule.onNodeWithText("Maximum amount per meal - we won't suggest anything above this")
            .assertExists()

        composeRule.onNodeWithText("Hard limit - meals above this won't be shown")
            .assertExists()

        composeRule.onNodeWithText("Set Your Maximum Budget (Optional)")
            .assertExists()

        // Label is testable
        composeRule.onNodeWithText("Maximum budget").assertExists()
    }

    @Test
    fun typingValidAmount_updatesValue() {
        var updatedValue: Int? = null

        composeRule.setContent {
            VeatoTheme {
                BudgetCapScreen(
                    budgetCap = null,
                    onUpdate = { updatedValue = it },
                    currentStep = 1,
                    totalSteps = 5,
                    onNext = {},
                    onPrevious = {}
                )
            }
        }

        // Type "20000"
        composeRule.onNode(hasSetTextAction()).performTextInput("20000")

        assertEquals(20000, updatedValue)
    }

    @Test
    fun typingInvalidAmount_showsErrorMessage() {
        composeRule.setContent {
            VeatoTheme {
                BudgetCapScreen(
                    budgetCap = null,
                    onUpdate = {},
                    currentStep = 1,
                    totalSteps = 5,
                    onNext = {},
                    onPrevious = {}
                )
            }
        }

        composeRule.onNode(hasSetTextAction()).performTextInput("-50")

        composeRule.onNodeWithText("Please enter a valid positive amount")
            .assertExists()
    }

    @Test
    fun blankAmount_showsNoLimitText() {
        composeRule.setContent {
            VeatoTheme {
                BudgetCapScreen(
                    budgetCap = null,
                    onUpdate = {},
                    currentStep = 1,
                    totalSteps = 5,
                    onNext = {},
                    onPrevious = {}
                )
            }
        }

        composeRule.onNodeWithText("Leave blank for no hard limit")
            .assertExists()
    }

    @Test
    fun skipButton_shows_whenBlank() {
        composeRule.setContent {
            VeatoTheme {
                BudgetCapScreen(
                    budgetCap = null,
                    onUpdate = {},
                    currentStep = 1,
                    totalSteps = 5,
                    onNext = {},
                    onPrevious = {}
                )
            }
        }

        composeRule.onNodeWithText("Skip").assertExists()
    }

    @Test
    fun skipButton_hidden_whenBudgetEntered() {
        composeRule.setContent {
            VeatoTheme {
                BudgetCapScreen(
                    budgetCap = 10000,
                    onUpdate = {},
                    currentStep = 1,
                    totalSteps = 5,
                    onNext = {},
                    onPrevious = {}
                )
            }
        }

        composeRule.onNodeWithText("Skip").assertDoesNotExist()
    }

    @Test
    fun nextButton_disabled_whenError() {
        composeRule.setContent {
            VeatoTheme {
                BudgetCapScreen(
                    budgetCap = null,
                    onUpdate = {},
                    currentStep = 1,
                    totalSteps = 5,
                    onNext = {},
                    onPrevious = {}
                )
            }
        }

        // Cause error
        composeRule.onNode(hasSetTextAction()).performTextInput("-10")

        composeRule.onNodeWithText("Next")
            .assertIsNotEnabled()
    }
}
