package com.example.veato.ui.onboarding.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.veato.data.model.Allergen
import com.example.veato.ui.theme.VeatoTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AllergiesScreenTest {

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
                AllergiesScreen(
                    selectedAllergies = emptyList(),
                    onUpdate = {},
                    currentStep = 1,
                    totalSteps = 5,
                    onNext = {},
                    onPrevious = {}
                )
            }
        }

        composeRule.onNodeWithText("Food Allergies").assertExists()
        composeRule.onNodeWithText("Critical for your safety - these will be completely excluded").assertExists()
        composeRule.onNodeWithText("Common Allergens").assertExists()
        composeRule.onNodeWithText("⚠️").assertExists()
        composeRule.onNodeWithText("Select ALL allergens to ensure your safety").assertExists()
    }

    @Test
    fun clickingAllergen_updatesSelectedList() {
        var selected: List<Allergen> = emptyList()

        composeRule.setContent {
            VeatoTheme {
                AllergiesScreen(
                    selectedAllergies = selected,
                    onUpdate = { selected = it },
                    currentStep = 1,
                    totalSteps = 5,
                    onNext = {},
                    onPrevious = {}
                )
            }
        }

        val first = Allergen.entries.filter { it != Allergen.CUSTOM }.first()
        val firstLabel = first.displayName

        composeRule.onNodeWithText(firstLabel).performClick()

        assertTrue(selected.contains(first))
    }
}
