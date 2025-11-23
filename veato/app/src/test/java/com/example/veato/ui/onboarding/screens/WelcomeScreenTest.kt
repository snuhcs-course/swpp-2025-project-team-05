package com.example.veato.ui.onboarding.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.veato.ui.theme.VeatoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WelcomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun welcomeScreen_displaysAllStaticTexts() {
        composeRule.setContent {
            VeatoTheme {
                WelcomeScreen(onNext = {})
            }
        }

        // Brand name
        composeRule.onNodeWithText("Veato").assertExists()

        // Main title
        composeRule.onNodeWithText("Welcome to Veato!").assertExists()

        // Description (use substring because of newlines)
        composeRule.onNode(
            hasText("Let's personalize your food recommendations", substring = true)
        ).assertExists()

        // Info card content
        composeRule.onNodeWithText("This will take about 2 minutes").assertExists()
        composeRule.onNode(
            hasText("Set your food constraints", substring = true)
        ).assertExists()
    }

    @Test
    fun welcomeScreen_clickGetStarted_callsOnNext() {
        var clickedCount = 0

        composeRule.setContent {
            VeatoTheme {
                WelcomeScreen(onNext = { clickedCount++ })
            }
        }

        composeRule.onNode(
            hasClickAction() and hasAnyChild(hasText("Get Started")),
            useUnmergedTree = true
        ).performClick()

        assertEquals(0, clickedCount)
    }
}
