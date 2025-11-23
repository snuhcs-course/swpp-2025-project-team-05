package com.example.veato.ui.onboarding.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.veato.data.model.BudgetRange
import com.example.veato.ui.theme.VeatoTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BudgetPreferenceScreenTest {

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
                BudgetPreferenceScreen(
                    budgetRange = BudgetRange(5000, 20000),
                    onUpdate = {},
                    currentStep = 1,
                    totalSteps = 5,
                    onNext = {},
                    onPrevious = {}
                )
            }
        }

        composeRule.onNodeWithText("Typical Budget").assertExists()
        composeRule.onNodeWithText("What's your usual budget range?").assertExists()
        composeRule.onNodeWithText("Meals in this range ranked higher (different from hard cap)").assertExists()
        composeRule.onNodeWithText("Your Preferred Price Range").assertExists()
        composeRule.onNodeWithText("Or choose a preset:").assertExists()
    }
}
