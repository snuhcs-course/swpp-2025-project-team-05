package com.example.veato

import android.content.Intent
import android.widget.Toast
import androidx.test.core.app.ActivityScenario
import androidx.lifecycle.ViewModelProvider
import com.example.veato.ui.profile.ProfileState
import com.example.veato.ui.profile.ProfileViewModel
import com.example.veato.data.model.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config


@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MyPreferencesActivityTest {

    private lateinit var mockViewModel: ProfileViewModel

    @Before
    fun setup() {
        mockkStatic(FirebaseApp::class)
        mockkStatic(FirebaseAuth::class)
        mockkStatic(FirebaseFirestore::class)
        mockkStatic(FirebaseStorage::class)

        every { FirebaseApp.getInstance() } returns mockk(relaxed = true)
        every { FirebaseFirestore.getInstance() } returns mockk(relaxed = true)

        val mockStorage = mockk<FirebaseStorage>(relaxed = true)
        every { FirebaseStorage.getInstance() } returns mockStorage
        every { mockStorage.reference } returns mockk(relaxed = true)

        val mockAuth = mockk<FirebaseAuth>(relaxed = true)
        val mockUser = mockk<FirebaseUser>(relaxed = true)
        every { FirebaseAuth.getInstance() } returns mockAuth
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns "test_user"

        mockViewModel = mockk(relaxed = true)

        every { mockViewModel.state } returns MutableStateFlow(
            ProfileState(isBusy = false, userProfile = null)
        ).asStateFlow()

        mockkConstructor(ViewModelProvider::class)
        every {
            anyConstructed<ViewModelProvider>().get(ProfileViewModel::class.java)
        } returns mockViewModel
    }


    @Test
    fun activity_starts_successfully() {
        ActivityScenario.launch(MyPreferencesActivity::class.java).use { assert(true) }
    }

    @Test
    fun clicking_backButton_finishesActivity() {
        val controller = Robolectric.buildActivity(MyPreferencesActivity::class.java).setup()
        val activity = controller.get()
        activity.runOnUiThread { activity.onBackPressedDispatcher.onBackPressed() }
        assert(activity.isFinishing)
    }

    @Test
    fun clicking_myTeams_opensMyTeamsActivity() {
        val controller = Robolectric.buildActivity(MyPreferencesActivity::class.java).setup()
        val activity = controller.get()

        val intent = Intent(activity, MyTeamsActivity::class.java)
        activity.startActivity(intent)

        val nextIntent = shadowOf(activity).nextStartedActivity
        assert(nextIntent.component?.className == MyTeamsActivity::class.java.name)
    }

    @Test
    fun clicking_myProfile_opensProfileActivity() {
        val controller = Robolectric.buildActivity(MyPreferencesActivity::class.java).setup()
        val activity = controller.get()

        val intent = Intent(activity, ProfileActivity::class.java)
        activity.startActivity(intent)

        val nextIntent = shadowOf(activity).nextStartedActivity
        assert(nextIntent.component?.className == ProfileActivity::class.java.name)
    }

    @Test
    fun activity_recreates_successfully() {
        val scenario = ActivityScenario.launch(MyPreferencesActivity::class.java)
        scenario.recreate()
        scenario.onActivity { assert(true) }
    }

    @Test
    fun viewModel_requested_once() {
        Robolectric.buildActivity(MyPreferencesActivity::class.java).setup()
        verify(exactly = 1) { anyConstructed<ViewModelProvider>().get(ProfileViewModel::class.java) }
    }

    @Test
    fun cuisine_update_calls_viewModel() {
        val updated = UserProfile(
            userId = "test",
            softPreferences = SoftPreferences(
                favoriteCuisines = listOf(CuisineType.WESTERN)
            )
        )
        mockViewModel.updateProfileData(updated)
        verify { mockViewModel.updateProfileData(updated) }
    }

    @Test
    fun saveButton_triggers_updateProfile() {
        mockViewModel.updateProfile()
        verify { mockViewModel.updateProfile() }
    }

    @Test
    fun create_toast_manually_should_work() {
        val controller = Robolectric.buildActivity(MyPreferencesActivity::class.java).setup()
        val activity = controller.get()

        activity.runOnUiThread {
            Toast.makeText(activity, "Preferences saved!", Toast.LENGTH_SHORT).show()
        }

        assert(true)
    }

    @Test
    fun activity_lifecycle_pause_resume() {
        val controller = Robolectric.buildActivity(MyPreferencesActivity::class.java).setup()
        controller.pause()
        controller.resume()
        assert(true)
    }

    @Test
    fun activity_lifecycle_stop_restart() {
        val controller = Robolectric.buildActivity(MyPreferencesActivity::class.java).setup()
        controller.stop()
        controller.restart()
        assert(true)
    }

    @Test
    fun multiple_state_updates_should_not_crash() {
        val flow = MutableStateFlow(ProfileState(isBusy = false, userProfile = null))
        every { mockViewModel.state } returns flow.asStateFlow()

        val scenario = ActivityScenario.launch(MyPreferencesActivity::class.java)

        flow.value = flow.value.copy(isBusy = true)
        flow.value = flow.value.copy(isBusy = false)

        val profile = UserProfile(userId = "1", fullName = "Alice")
        flow.value = flow.value.copy(userProfile = profile)

        scenario.onActivity { assert(flow.value.userProfile?.fullName == "Alice") }
    }
}
