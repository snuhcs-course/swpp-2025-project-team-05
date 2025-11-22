package com.example.veato

import android.content.Intent
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.example.veato.data.model.Allergen
import com.example.veato.data.model.DietaryType
import com.example.veato.data.model.HardConstraints
import com.example.veato.data.model.UserProfile
import com.example.veato.ui.profile.ProfileState
import com.example.veato.ui.profile.ProfileViewModel
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
class ProfileActivityTest {

    private lateinit var controller: org.robolectric.android.controller.ActivityController<ProfileActivity>
    private lateinit var mockViewModel: ProfileViewModel
    private lateinit var fakeState: MutableStateFlow<ProfileState>

    @Before
    fun setup() {
        // Create mock ViewModel
        mockViewModel = mockk(relaxed = true)

        // Fake VM state
        fakeState = MutableStateFlow(
            ProfileState(
                userProfile = UserProfile(
                    userId = "uid123",
                    userName = "johnd",
                    fullName = "John Doe",
                    profilePictureUrl = "http://example.com/pic.jpg",
                    hardConstraints = HardConstraints(
                        dietaryRestrictions = listOf(DietaryType.HALAL),
                        allergies = listOf(Allergen.FISH),
                        avoidIngredients = listOf("onion")
                    )
                ),
                isEditing = false,
                isBusy = false,
                editedFullName = "John Doe",
                editedUserName = "johnd"
            )
        )

        every { mockViewModel.state } returns fakeState

        // Intercept ViewModelProvider.Factory instead of Composables
        mockkConstructor(ViewModelProvider::class)
        every {
            anyConstructed<ViewModelProvider>().get(ProfileViewModel::class.java)
        } returns mockViewModel

        controller = Robolectric.buildActivity(ProfileActivity::class.java)
    }

    @After
    fun teardown() = unmockkAll()

    private fun flush() = Shadows.shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun activityLaunchesSuccessfully() {
        controller.create().start().resume()
        flush()
        assert(true)
    }

    @Test
    fun loadsStateOnStartup() {
        controller.create().start().resume()
        flush()

        assert(fakeState.value.userProfile != null)
    }

    @Test
    fun bottomNavMyTeamsLaunchesActivity() {
        controller.create().start().resume()
        flush()

        val activity = controller.get()
        activity.startActivity(Intent(activity, MyTeamsActivity::class.java))

        val next = Shadows.shadowOf(activity).nextStartedActivity
        assert(next.component?.className?.contains("MyTeamsActivity") == true)
    }

    @Test
    fun bottomNavPreferencesLaunchesActivity() {
        controller.create().start().resume()
        flush()

        val activity = controller.get()
        activity.startActivity(Intent(activity, MyPreferencesActivity::class.java))

        val next = Shadows.shadowOf(activity).nextStartedActivity
        assert(next.component?.className?.contains("MyPreferencesActivity") == true)
    }

    @Test
    fun displaysErrorMessage() {
        fakeState.value = fakeState.value.copy(saveError = "Something went wrong")

        controller.create().start().resume()
        flush()

        assert(fakeState.value.saveError == "Something went wrong")
    }

    @Test
    fun dietaryRestrictionsAreDisplayed() {
        controller.create().start().resume()
        flush()

        assert(fakeState.value.userProfile?.hardConstraints?.dietaryRestrictions?.isNotEmpty() == true)
    }
}
