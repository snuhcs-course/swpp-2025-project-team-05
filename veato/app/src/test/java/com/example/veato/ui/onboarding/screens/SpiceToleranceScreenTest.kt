package com.example.veato.ui.onboarding.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.mutableStateOf
import com.example.veato.data.model.SpiceLevel
import com.example.veato.ui.theme.VeatoTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SpiceToleranceScreenTest {

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
                SpiceToleranceScreen(
                    spiceLevel = SpiceLevel.MEDIUM,
                    onUpdate = {},
                    currentStep = 1,
                    totalSteps = 5,
                    onNext = {},
                    onPrevious = {}
                )
            }
        }

        composeRule.onNodeWithText("Spice Preference").assertExists()
        composeRule.onNodeWithText("How spicy do you like your food?").assertExists()
        composeRule.onNodeWithText("Helps rank menu options - doesn't exclude mild food").assertExists()
        composeRule.onNodeWithText("Your Spice Tolerance").assertExists()

        // Slider label
        composeRule.onNodeWithText(SpiceLevel.MEDIUM.displayName).assertExists()
    }

    @Test
    fun description_matches_selected_level() {
        composeRule.setContent {
            VeatoTheme {
                SpiceToleranceScreen(
                    spiceLevel = SpiceLevel.EXTRA,
                    onUpdate = {},
                    currentStep = 1,
                    totalSteps = 5,
                    onNext = {},
                    onPrevious = {}
                )
            }
        }

        composeRule.onNodeWithText("The spicier the better").assertExists()
    }

    @Test
    fun changing_spiceLevel_updates_description() {
        val current = mutableStateOf(SpiceLevel.LOW)

        composeRule.setContent {
            VeatoTheme {
                SpiceToleranceScreen(
                    spiceLevel = current.value,
                    onUpdate = { current.value = it },
                    currentStep = 1,
                    totalSteps = 5,
                    onNext = {},
                    onPrevious = {}
                )
            }
        }

        composeRule.onNodeWithText("A little kick is okay, but keep it mild")
            .assertExists()

        // Update the state INSIDE Compose
        composeRule.runOnUiThread {
            current.value = SpiceLevel.HIGH
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("I love spicy food")
            .assertExists()
    }
}
