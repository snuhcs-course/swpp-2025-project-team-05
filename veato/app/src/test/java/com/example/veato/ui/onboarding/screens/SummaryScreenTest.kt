package com.example.veato.ui.onboarding.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.veato.data.model.*
import com.example.veato.ui.theme.VeatoTheme
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SummaryScreenTest {

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

    // ---------------------------------------------------------
    // Fake profile for testing
    // ---------------------------------------------------------
    private fun mockProfile() = UserProfile(
        hardConstraints = HardConstraints(
            dietaryRestrictions = listOf(DietaryType.VEGAN),
            allergies = listOf(Allergen.EGGS, Allergen.DAIRY),
            avoidIngredients = listOf("Mushroom", "Cilantro")
        ),
        softPreferences = SoftPreferences(
            favoriteCuisines = listOf(CuisineType.KOREAN),
            spiceTolerance = SpiceLevel.HIGH
        )
    )


    @Test
    fun screen_displays_static_texts() {
        composeRule.setContent {
            VeatoTheme {
                SummaryScreen(
                    profile = mockProfile(),
                    onSave = {},
                    onEdit = {},
                    currentStep = 5,
                    totalSteps = 5,
                    isSaving = false,
                    saveError = null
                )
            }
        }

        composeRule.onNodeWithText("Review & Finish").assertExists()
        composeRule.onNodeWithText("Review your preferences before completing setup").assertExists()
        composeRule.onNodeWithText("Hard Constraints").assertExists()
        composeRule.onNodeWithText("Preferences").assertExists()
        composeRule.onNodeWithText("Complete Setup").assertExists()
    }


    @Test
    fun softPreferences_itemsDisplayed() {
        composeRule.setContent {
            VeatoTheme {
                SummaryScreen(
                    profile = mockProfile(),
                    onSave = {},
                    onEdit = {},
                    currentStep = 5,
                    totalSteps = 5,
                    isSaving = false,
                    saveError = null
                )
            }
        }

        composeRule.onNodeWithText("Cuisines:").assertExists()
        composeRule.onNodeWithText("Korean").assertExists()

        composeRule.onNodeWithText("Spice:").assertExists()
        composeRule.onNodeWithText(SpiceLevel.HIGH.displayName).assertExists()
    }


    @Test
    fun clickingSave_callsOnSave() {
        var called = false

        composeRule.setContent {
            VeatoTheme {
                SummaryScreen(
                    profile = mockProfile(),
                    onSave = { called = true },
                    onEdit = {},
                    currentStep = 5,
                    totalSteps = 5,
                    isSaving = false,
                    saveError = null
                )
            }
        }

        composeRule.onNodeWithText("Complete Setup").performClick()
        Assert.assertTrue(called)
    }

    @Test
    fun errorMessage_isDisplayed_whenSaveErrorExists() {
        composeRule.setContent {
            VeatoTheme {
                SummaryScreen(
                    profile = mockProfile(),
                    onSave = {},
                    onEdit = {},
                    currentStep = 5,
                    totalSteps = 5,
                    isSaving = false,
                    saveError = "Network error"
                )
            }
        }

        composeRule.onNodeWithText("Error: Network error").assertExists()
    }
}
