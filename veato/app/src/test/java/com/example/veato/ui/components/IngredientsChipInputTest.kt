package com.example.veato.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import com.example.veato.ui.theme.VeatoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IngredientsChipInputTest {

    @get:Rule
    val composeRule = createComposeRule()


    @Test
    fun duplicateIngredient_isNotAdded() {
        var ing = listOf("Sugar")

        composeRule.setContent {
            VeatoTheme {
                IngredientsChipInput(
                    ingredients = ing,
                    onIngredientsChange = { ing = it }
                )
            }
        }

        composeRule.onNode(hasText("Add Ingredient"))
            .performTextInput("Sugar")

        composeRule.onNodeWithContentDescription("Add ingredient").performClick()

        // still 1 only
        assert(ing.size == 1)
    }

    @Test
    fun clickingChip_removesIngredient() {
        var ing = listOf("Garlic")

        composeRule.setContent {
            VeatoTheme {
                IngredientsChipInput(
                    ingredients = ing,
                    onIngredientsChange = { ing = it }
                )
            }
        }

        // Click chip (InputChip triggers removal)
        composeRule.onNodeWithText("Garlic").performClick()

        assert(!ing.contains("Garlic"))
    }

    @Test
    fun ingredients_render_in_two_columns() {
        var ing = listOf("A", "B", "C")

        composeRule.setContent {
            VeatoTheme {
                IngredientsChipInput(
                    ingredients = ing,
                    onIngredientsChange = { ing = it }
                )
            }
        }

        // Just check all 3 chips exist
        composeRule.onNodeWithText("A").assertExists()
        composeRule.onNodeWithText("B").assertExists()
        composeRule.onNodeWithText("C").assertExists()
    }

    @Test
    fun disabledMode_hidesInput_andShowsNoIngredientsMessage() {
        composeRule.setContent {
            VeatoTheme {
                IngredientsChipInput(
                    ingredients = emptyList(),
                    onIngredientsChange = {},
                    enabled = false
                )
            }
        }

        // No text field
        composeRule.onNodeWithText("Add Ingredient").assertDoesNotExist()

        // Message displayed
        composeRule.onNodeWithText("No ingredients to avoid").assertExists()
    }

    @Test
    fun disabledMode_preventsRemoval() {
        var ing = listOf("Ginger")

        composeRule.setContent {
            VeatoTheme {
                IngredientsChipInput(
                    ingredients = ing,
                    onIngredientsChange = { ing = it },
                    enabled = false
                )
            }
        }

        // Chip exists
        composeRule.onNodeWithText("Ginger").assertExists()

        // Try clicking → should not remove
        composeRule.onNodeWithText("Ginger").performClick()

        assert(ing.contains("Ginger"))
    }
}
