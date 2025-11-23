package com.example.veato.ui.onboarding.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.veato.ui.theme.VeatoTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AvoidIngredientsScreenTest {

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
                AvoidIngredientsScreen(
                    avoidList = emptyList(),
                    onUpdate = {},
                    currentStep = 1,
                    totalSteps = 5,
                    onNext = {},
                    onPrevious = {}
                )
            }
        }

        composeRule.onNodeWithText("Ingredients to Avoid").assertExists()
        composeRule.onNodeWithText("Foods you absolutely refuse to eat").assertExists()
        composeRule.onNodeWithText("These will be completely excluded from recommendations").assertExists()

        // Label ("Add ingredient") is testable
        composeRule.onNodeWithText("Add ingredient").assertExists()
    }

    @Test
    fun typingIngredient_andClickingAdd_updatesList() {
        var updatedList: List<String> = emptyList()

        composeRule.setContent {
            VeatoTheme {
                AvoidIngredientsScreen(
                    avoidList = updatedList,
                    onUpdate = { updatedList = it },
                    currentStep = 1,
                    totalSteps = 5,
                    onNext = {},
                    onPrevious = {}
                )
            }
        }

        // 1. Type into real editable field
        composeRule.onNode(hasSetTextAction())
            .performTextInput("Mushroom")

        // 2. Click trailing icon
        composeRule.onNodeWithContentDescription("Add ingredient")
            .performClick()

        // 3. Verify update
        assertEquals(listOf("Mushroom"), updatedList)
    }

    @Test
    fun avoidList_showsChips() {
        composeRule.setContent {
            VeatoTheme {
                AvoidIngredientsScreen(
                    avoidList = listOf("Onion", "Garlic"),
                    onUpdate = {},
                    currentStep = 1,
                    totalSteps = 5,
                    onNext = {},
                    onPrevious = {}
                )
            }
        }

        composeRule.onNodeWithText("Avoiding (2)").assertExists()
        composeRule.onNodeWithText("Onion").assertExists()
        composeRule.onNodeWithText("Garlic").assertExists()
    }

    @Test
    fun clickingPrevious_callsCallback() {
        var called = false

        composeRule.setContent {
            VeatoTheme {
                AvoidIngredientsScreen(
                    avoidList = emptyList(),
                    onUpdate = {},
                    currentStep = 1,
                    totalSteps = 5,
                    onNext = {},
                    onPrevious = { called = true }
                )
            }
        }

        composeRule.onNodeWithText("Back", substring = true, useUnmergedTree = true)
            .performClick()

        assertTrue(called)
    }

    @Test
    fun skipButton_shows_whenListEmpty() {
        composeRule.setContent {
            VeatoTheme {
                AvoidIngredientsScreen(
                    avoidList = emptyList(),
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
    fun skipButton_hidden_whenHasItems() {
        composeRule.setContent {
            VeatoTheme {
                AvoidIngredientsScreen(
                    avoidList = listOf("Broccoli"),
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
}
