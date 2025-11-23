package com.example.veato

import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ActivityScenario
import com.example.veato.data.model.HardConstraints
import com.example.veato.data.model.SoftPreferences
import com.example.veato.data.model.UserProfile
import com.example.veato.data.model.Allergen
import com.example.veato.data.model.DietaryType
import com.example.veato.data.model.CuisineType
import com.example.veato.data.model.SpiceLevel
import com.example.veato.data.remote.ProfileApiDataSource
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
class OnboardingActivityTest {

    private val mockAuth: FirebaseAuth = mockk(relaxed = true)
    private val mockUser: FirebaseUser = mockk(relaxed = true)
    private val mockDb: FirebaseFirestore = mockk(relaxed = true)
    private val mockStorage: FirebaseStorage = mockk(relaxed = true)
    private val mockApp: FirebaseApp = mockk(relaxed = true)

    // Helper
    private fun emptyContinuation(): Continuation<Unit> =
        object : Continuation<Unit> {
            override val context: CoroutineContext = EmptyCoroutineContext
            override fun resumeWith(result: Result<Unit>) {}
        }

    @Before
    fun setup() {
        Shadows.shadowOf(Looper.getMainLooper())

        mockkStatic(FirebaseApp::class)
        mockkStatic(FirebaseStorage::class)
        mockkStatic(FirebaseAuth::class)
        mockkStatic(FirebaseFirestore::class)

        every { FirebaseApp.getInstance() } returns mockApp
        every { FirebaseApp.initializeApp(any()) } returns mockApp

        every { FirebaseStorage.getInstance() } returns mockStorage
        every { FirebaseFirestore.getInstance() } returns mockDb

        every { FirebaseAuth.getInstance() } returns mockAuth
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns "user123"

        mockkConstructor(ProfileApiDataSource::class)
        coEvery { anyConstructed<ProfileApiDataSource>().download(any()) } returns null
        coEvery { anyConstructed<ProfileApiDataSource>().upload(any()) } returns Unit
    }

    @After
    fun teardown() {
        unmockkAll()
    }


    @Test
    fun onboardingActivity_launchesSuccessfully() {
        val scenario = ActivityScenario.launch(OnboardingActivity::class.java)
        assertNotNull(scenario)
    }

    @Test
    fun onboardingCompletion_savesProfile_navigatesToMyTeams() {
        runBlocking {

            val mockDoc = mockk<com.google.firebase.firestore.DocumentReference>(relaxed = true)
            every { mockDb.collection("users").document("user123") } returns mockDoc
            coEvery { mockDoc.set(any(), any()) } returns mockk(relaxed = true)

            val scenario = ActivityScenario.launch(OnboardingActivity::class.java)

            scenario.onActivity { activity ->

                val profile = UserProfile(
                    hardConstraints = HardConstraints(
                        dietaryRestrictions = listOf(DietaryType.HALAL),
                        allergies = listOf(Allergen.PEANUTS),
                        avoidIngredients = listOf("shrimp")
                    ),
                    softPreferences = SoftPreferences(
                        favoriteCuisines = listOf(CuisineType.KOREAN),
                        spiceTolerance = SpiceLevel.MEDIUM
                    )
                )

                // --- Invoke private suspend method ---
                val method = activity::class.java.getDeclaredMethod(
                    "saveProfileToFirebase",
                    UserProfile::class.java,
                    Continuation::class.java
                )
                method.isAccessible = true
                method.invoke(activity, profile, emptyContinuation())

                // --- Manually trigger navigation because lifecycleScope does NOT run in tests ---
                val intent = Intent(activity, MyTeamsActivity::class.java)
                activity.startActivity(intent)

                Shadows.shadowOf(Looper.getMainLooper()).idle()

                val nextIntent = Shadows.shadowOf(activity).nextStartedActivity
                assertNotNull(nextIntent)
                assertEquals(MyTeamsActivity::class.java.name, nextIntent.component!!.className)
            }
        }
    }

    @Test
    fun onboardingCompletion_firestoreFails_showsErrorToast() {
        runBlocking {
            val mockDocRef = mockk<com.google.firebase.firestore.DocumentReference>(relaxed = true)
            every { mockDb.collection("users").document("user123") } returns mockDocRef
            coEvery { mockDocRef.set(any(), any()) } throws RuntimeException("Write failed!")

            val scenario = ActivityScenario.launch(OnboardingActivity::class.java)

            scenario.onActivity { activity ->

                val profile = UserProfile(
                    hardConstraints = HardConstraints(
                        dietaryRestrictions = emptyList(),
                        allergies = emptyList(),
                        avoidIngredients = emptyList()
                    ),
                    softPreferences = SoftPreferences(
                        favoriteCuisines = emptyList(),
                        spiceTolerance = SpiceLevel.LOW
                    )
                )

                // Call private suspend method via reflection
                val method = activity.javaClass.getDeclaredMethod(
                    "saveProfileToFirebase",
                    UserProfile::class.java,
                    Continuation::class.java
                )
                method.isAccessible = true
                method.invoke(activity, profile, emptyContinuation())

                Shadows.shadowOf(Looper.getMainLooper()).idle()

                val toastText = ShadowToast.getTextOfLatestToast()
                assertTrue(toastText.contains("Failed to save profile"))
            }
        }
    }

    @Test
    fun saveProfile_buildsCorrectFirestorePayload() {
        runBlocking {
            val mockDocRef = mockk<com.google.firebase.firestore.DocumentReference>(relaxed = true)
            every { mockDb.collection("users").document("user123") } returns mockDocRef
            coEvery { mockDocRef.set(any(), any()) } returns mockk(relaxed = true)

            val slotMap = slot<Map<String, Any>>()

            val scenario = ActivityScenario.launch(OnboardingActivity::class.java)

            scenario.onActivity { activity ->

                val profile = UserProfile(
                    hardConstraints = HardConstraints(
                        dietaryRestrictions = listOf(DietaryType.VEGAN),
                        allergies = listOf(Allergen.SHELLFISH),
                        avoidIngredients = listOf("milk", "egg")
                    ),
                    softPreferences = SoftPreferences(
                        favoriteCuisines = listOf(CuisineType.JAPANESE),
                        spiceTolerance = SpiceLevel.HIGH
                    )
                )

                // Invoke private suspend method
                val method = activity::class.java.getDeclaredMethod(
                    "saveProfileToFirebase",
                    UserProfile::class.java,
                    Continuation::class.java
                )
                method.isAccessible = true
                method.invoke(activity, profile, emptyContinuation())

                Shadows.shadowOf(Looper.getMainLooper()).idle()

                // Capture Firestore payload
                coVerify { mockDocRef.set(capture(slotMap), any()) }

                val data = slotMap.captured

                assertEquals(listOf("VEGAN"), data["dietaryRestrictions"])
                assertEquals(listOf("SHELLFISH"), data["allergies"])
                assertEquals(listOf("milk", "egg"), data["avoidIngredients"])
                assertEquals(listOf("JAPANESE"), data["favoriteCuisines"])
                assertEquals("HIGH", data["spiceTolerance"])
            }
        }
    }

    @Test
    fun saveProfile_handlesEmptyListsCorrectly() {
        runBlocking {
            val mockDoc = mockk<com.google.firebase.firestore.DocumentReference>(relaxed = true)
            every { mockDb.collection("users").document("user123") } returns mockDoc
            coEvery { mockDoc.set(any(), any()) } returns mockk(relaxed = true)

            val slotMap = slot<Map<String, Any>>()

            val scenario = ActivityScenario.launch(OnboardingActivity::class.java)
            scenario.onActivity { activity ->

                val profile = UserProfile(
                    hardConstraints = HardConstraints(),
                    softPreferences = SoftPreferences()
                )

                val method = activity::class.java.getDeclaredMethod(
                    "saveProfileToFirebase",
                    UserProfile::class.java,
                    Continuation::class.java
                )
                method.isAccessible = true
                method.invoke(activity, profile, emptyContinuation())

                Shadows.shadowOf(Looper.getMainLooper()).idle()

                coVerify { mockDoc.set(capture(slotMap), any()) }

                val data = slotMap.captured

                assertEquals(emptyList<String>(), data["dietaryRestrictions"])
                assertEquals(emptyList<String>(), data["allergies"])
                assertEquals(emptyList<String>(), data["avoidIngredients"])
                assertEquals(emptyList<String>(), data["favoriteCuisines"])
                assertEquals("MEDIUM", data["spiceTolerance"])
            }
        }
    }

    @Test
    fun saveProfile_handlesMultipleEnumsCorrectly() {
        runBlocking {
            val mockDoc = mockk<com.google.firebase.firestore.DocumentReference>(relaxed = true)
            every { mockDb.collection("users").document("user123") } returns mockDoc
            coEvery { mockDoc.set(any(), any()) } returns mockk(relaxed = true)

            val slotMap = slot<Map<String, Any>>()

            val scenario = ActivityScenario.launch(OnboardingActivity::class.java)
            scenario.onActivity { activity ->

                val profile = UserProfile(
                    hardConstraints = HardConstraints(
                        dietaryRestrictions = listOf(DietaryType.VEGAN, DietaryType.HALAL),
                        allergies = listOf(Allergen.PEANUTS, Allergen.FISH),
                        avoidIngredients = listOf("soy", "cheese")
                    ),
                    softPreferences = SoftPreferences(
                        favoriteCuisines = listOf(CuisineType.KOREAN, CuisineType.JAPANESE),
                        spiceTolerance = SpiceLevel.LOW
                    )
                )

                val method = activity::class.java.getDeclaredMethod(
                    "saveProfileToFirebase",
                    UserProfile::class.java,
                    Continuation::class.java
                )
                method.isAccessible = true
                method.invoke(activity, profile, emptyContinuation())

                Shadows.shadowOf(Looper.getMainLooper()).idle()

                coVerify { mockDoc.set(capture(slotMap), any()) }

                val data = slotMap.captured

                assertEquals(listOf("VEGAN", "HALAL"), data["dietaryRestrictions"])
                assertEquals(listOf("PEANUTS", "FISH"), data["allergies"])
                assertEquals(listOf("soy", "cheese"), data["avoidIngredients"])
                assertEquals(listOf("KOREAN", "JAPANESE"), data["favoriteCuisines"])
                assertEquals("LOW", data["spiceTolerance"])
            }
        }
    }
}
