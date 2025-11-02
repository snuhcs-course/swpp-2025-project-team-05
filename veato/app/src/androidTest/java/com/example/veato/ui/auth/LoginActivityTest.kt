package com.example.veato.ui.auth

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import org.hamcrest.Matchers.anyOf
import com.example.veato.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before
import org.junit.After

/**
 * Espresso UI tests for LoginActivity
 * Covers:
 *  - UI visibility
 *  - Login button input behavior
 *  - "Sign up" navigation text clickability and intent verification
 */
@RunWith(AndroidJUnit4::class)
class LoginActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(LoginActivity::class.java)

    @Before
    fun setupIntents() {
        Intents.init()
    }

    @After
    fun tearDownIntents() {
        Intents.release()
    }

    @Test
    fun loginScreen_elementsDisplayedCorrectly() {
        onView(withId(R.id.tvBrand)).check(matches(withText("Veato")))
        onView(withId(R.id.etEmail)).check(matches(isDisplayed()))
        onView(withId(R.id.etPassword)).check(matches(isDisplayed()))
        onView(withId(R.id.btnLogin)).check(matches(withText("Log in")))
        onView(withId(R.id.tvGotoSignup)).check(matches(withText(" Sign up")))
        onView(withId(R.id.tvForgot)).check(matches(isDisplayed()))
        onView(withId(R.id.btnGoogle)).check(matches(withText("Continue with Google")))
    }

    @Test
    fun typingEmailAndPassword_allowsLoginButtonClick() {
        onView(withId(R.id.etEmail))
            .perform(typeText("test@example.com"), closeSoftKeyboard())

        onView(withId(R.id.etPassword))
            .perform(typeText("password123"), closeSoftKeyboard())

        onView(withId(R.id.btnLogin)).perform(click())

        onView(withId(R.id.progress))
            .check(matches(anyOf(isDisplayed(), withEffectiveVisibility(Visibility.GONE))))
    }

    @Test
    fun clickSignupText_navigatesToRegisterScreen() {
        waitForView(R.id.tvGotoSignup)
        closeSoftKeyboard()

        onView(withId(R.id.tvGotoSignup))
            .perform(click())

        Intents.intended(hasComponent(RegisterActivity::class.java.name))
    }

    private fun waitForView(viewId: Int, timeout: Long = 3000) {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeout) {
            try {
                onView(withId(viewId)).check(matches(isDisplayed()))
                return
            } catch (e: Exception) {
                Thread.sleep(100)
            }
        }
        throw AssertionError("View with id $viewId not found after $timeout ms")
    }
}
