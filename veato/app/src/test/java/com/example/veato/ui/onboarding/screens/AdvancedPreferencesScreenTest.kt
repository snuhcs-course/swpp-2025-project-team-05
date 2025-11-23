package com.example.veato.ui.onboarding.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.veato.data.model.MealType
import com.example.veato.data.model.PortionSize
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
class AdvancedPreferencesScreenTest {

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
                AdvancedPreferencesScreen(
                    mealTypes = emptyList(),
                    portionSize = null,
                    onUpdateMealTypes = {},
                    onUpdatePortionSize = {},
                    currentStep = 1,
                    totalSteps = 5,
                    onNext = {},
                    onPrevious = {}
                )
            }
        }

        composeRule.onNodeWithText("Advanced Preferences").assertExists()
        composeRule.onNodeWithText("Optional: Fine-tune your meal recommendations").assertExists()
        composeRule.onNodeWithText("Preferred Meal Types").assertExists()
        composeRule.onNodeWithText("Preferred Portion Size").assertExists()
        composeRule.onNodeWithText("No preference selected").assertExists()
    }

    @Test
    fun clickingMealType_updatesSelectedList() {
        var selected: List<MealType> = emptyList()

        composeRule.setContent {
            VeatoTheme {
                AdvancedPreferencesScreen(
                    mealTypes = selected,
                    portionSize = null,
                    onUpdateMealTypes = { selected = it },
                    onUpdatePortionSize = {},
                    currentStep = 1,
                    totalSteps = 5,
                    onNext = {},
                    onPrevious = {}
                )
            }
        }

        val firstLabel = "${MealType.entries.first().koreanName} ${MealType.entries.first().displayName}"

        composeRule.onNodeWithText(firstLabel).performClick()

        assert(selected.contains(MealType.entries.first()))
    }

    @Test
    fun skipButton_shows_whenNothingSelected() {
        composeRule.setContent {
            VeatoTheme {
                AdvancedPreferencesScreen(
                    mealTypes = emptyList(),
                    portionSize = null,
                    onUpdateMealTypes = {},
                    onUpdatePortionSize = {},
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
    fun skipButton_hidden_whenPreferencesSelected() {
        composeRule.setContent {
            VeatoTheme {
                AdvancedPreferencesScreen(
                    mealTypes = listOf(MealType.RICE_BASED),
                    portionSize = PortionSize.MEDIUM,
                    onUpdateMealTypes = {},
                    onUpdatePortionSize = {},
                    currentStep = 1,
                    totalSteps = 5,
                    onNext = {},
                    onPrevious = {}
                )
            }
        }

        composeRule.onNodeWithText("Skip").assertDoesNotExist()
    }
}
