package com.example.veato

import android.content.Intent
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MealPollActivityTest {

    @Test
    fun mealPollActivity_displaysCorrectTitle() {
        // Prepare intent with teamId
        val intent = Intent(
            androidx.test.core.app.ApplicationProvider.getApplicationContext(),
            MealPollActivity::class.java
        ).apply {
            putExtra("teamId", "Team123")
        }

        ActivityScenario.launch<MealPollActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->

                val titleView = activity.findViewById<TextView>(R.id.tvPollTitle)
                assertNotNull("Title TextView not found", titleView)

                assertEquals(
                    "Meal Poll for Team: Team123",
                    titleView.text.toString()
                )
            }
        }
    }

    @Test
    fun clickingBackButton_finishesActivity() {
        val intent = Intent(
            androidx.test.core.app.ApplicationProvider.getApplicationContext(),
            MealPollActivity::class.java
        ).apply {
            putExtra("teamId", "TeamABC")
        }

        ActivityScenario.launch<MealPollActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val btnBack = activity.findViewById<Button>(R.id.btnBack)
                assertNotNull(btnBack)

                btnBack.performClick()

                assertTrue(
                    "Activity should be finishing after back button click",
                    activity.isFinishing
                )
            }
        }
    }
}
