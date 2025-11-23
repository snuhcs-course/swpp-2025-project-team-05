package com.example.veato.ui.onboarding.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.veato.data.model.CuisineType
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
class FavoriteCuisinesScreenTest {

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
                FavoriteCuisinesScreen(
                    selectedCuisines = emptyList(),
                    onUpdate = {},
                    currentStep = 1,
                    totalSteps = 5,
                    onNext = {},
                    onPrevious = {}
                )
            }
        }

        composeRule.onNodeWithText("Favorite Cuisines").assertExists()
        composeRule.onNodeWithText("What do you usually enjoy?").assertExists()
        composeRule.onNodeWithText("Helps us rank recommendations - doesn't exclude others").assertExists()
        composeRule.onNodeWithText("Select Your Favorites").assertExists()
    }

    @Test
    fun clickingCuisine_updatesSelectedList() {
        var updated: List<CuisineType> = emptyList()

        composeRule.setContent {
            VeatoTheme {
                FavoriteCuisinesScreen(
                    selectedCuisines = updated,
                    onUpdate = { updated = it },
                    currentStep = 1,
                    totalSteps = 5,
                    onNext = {},
                    onPrevious = {}
                )
            }
        }

        val first = CuisineType.entries.first()
        val label = "${first.koreanName} ${first.displayName}"

        composeRule.onNodeWithText(label).performClick()

        assertTrue(updated.contains(first))
    }

    @Test
    fun selectedCount_showsWhenSelectionExists() {
        val selected = listOf(CuisineType.entries.first())

        composeRule.setContent {
            VeatoTheme {
                FavoriteCuisinesScreen(
                    selectedCuisines = selected,
                    onUpdate = {},
                    currentStep = 1,
                    totalSteps = 5,
                    onNext = {},
                    onPrevious = {}
                )
            }
        }

        composeRule.onNodeWithText("Selected 1 cuisine").assertExists()
    }

    @Test
    fun skipButton_visibleWhenEmptySelection() {
        composeRule.setContent {
            VeatoTheme {
                FavoriteCuisinesScreen(
                    selectedCuisines = emptyList(),
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
    fun skipButton_hiddenWhenHasSelection() {
        composeRule.setContent {
            VeatoTheme {
                FavoriteCuisinesScreen(
                    selectedCuisines = listOf(CuisineType.KOREAN),
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
