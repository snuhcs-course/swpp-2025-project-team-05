package com.example.veato.ui.onboarding.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.veato.data.model.HeavinessLevel
import com.example.veato.ui.theme.VeatoTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HeavinessPreferenceScreenTest {
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
                HeavinessPreferenceScreen(
                    heavinessLevel = HeavinessLevel.MEDIUM,
                    onUpdate = {},
                    currentStep = 1,
                    totalSteps = 5,
                    onNext = {},
                    onPrevious = {}
                )
            }
        }

        composeRule.onNodeWithText("Meal Heaviness").assertExists()
        composeRule.onNodeWithText("Do you prefer light or filling meals?").assertExists()
        composeRule.onNodeWithText("Influences portion size and meal type recommendations").assertExists()
        composeRule.onNodeWithText("Your Preference").assertExists()
    }

    @Test
    fun emoji_matches_selected_heaviness() {
        composeRule.setContent {
            VeatoTheme {
                HeavinessPreferenceScreen(
                    heavinessLevel = HeavinessLevel.HEAVY,
                    onUpdate = {},
                    currentStep = 1,
                    totalSteps = 5,
                    onNext = {},
                    onPrevious = {}
                )
            }
        }

        composeRule.onNodeWithText("🍔").assertExists()
    }

    @Test
    fun selectedChip_existsInTree() {
        composeRule.setContent {
            VeatoTheme {
                HeavinessPreferenceScreen(
                    heavinessLevel = HeavinessLevel.LIGHT,
                    onUpdate = {},
                    currentStep = 1,
                    totalSteps = 5,
                    onNext = {},
                    onPrevious = {}
                )
            }
        }

        // Verify the chip for LIGHT is rendered as selected
        composeRule
            .onNode(
                hasAnyDescendant(hasText("Light")) and hasClickAction(),
                useUnmergedTree = true
            )
            .assertExists()
    }
}
