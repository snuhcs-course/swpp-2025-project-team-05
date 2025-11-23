package com.example.veato.ui.onboarding.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.veato.data.model.DietaryType
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
class DietaryRestrictionsScreenTest {

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
                DietaryRestrictionsScreen(
                    selectedRestrictions = emptyList(),
                    onUpdate = {},
                    currentStep = 1,
                    totalSteps = 5,
                    onNext = {},
                    onPrevious = {}
                )
            }
        }

        composeRule.onNodeWithText("Dietary Restrictions").assertExists()
        composeRule.onNodeWithText("Select all that apply to you").assertExists()
        composeRule.onNodeWithText("We'll NEVER recommend meals that violate these").assertExists()
        composeRule.onNodeWithText("Your Restrictions").assertExists()
    }

    @Test
    fun clickingRestriction_updatesSelectedList() {
        var updated: List<DietaryType> = emptyList()

        composeRule.setContent {
            VeatoTheme {
                DietaryRestrictionsScreen(
                    selectedRestrictions = updated,
                    onUpdate = { updated = it },
                    currentStep = 1,
                    totalSteps = 5,
                    onNext = {},
                    onPrevious = {}
                )
            }
        }

        val first = DietaryType.entries.first { it != DietaryType.CUSTOM }
        val firstLabel = first.displayName

        composeRule.onNodeWithText(firstLabel).performClick()

        assertTrue(updated.contains(first))
    }
}
