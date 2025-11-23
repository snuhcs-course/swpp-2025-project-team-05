package com.example.veato.ui.onboarding

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.example.veato.data.model.*
import com.example.veato.data.repository.UserProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var repository: UserProfileRepository
    private lateinit var viewModel: OnboardingViewModel
    private val testUserId = "test_user_123"

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        repository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---------------------------------------------------------
    // INITIALIZATION
    // ---------------------------------------------------------

    @Test
    fun initialization_setsCorrectInitialState() = runTest {
        viewModel = OnboardingViewModel(repository, testUserId)

        viewModel.state.test {
            val s = awaitItem()
            assertEquals(OnboardingScreen.Welcome, s.currentScreen)
            assertEquals(testUserId, s.profileDraft.userId)
            assertFalse(s.isSaving)
            assertFalse(s.isComplete)
            assertNull(s.saveError)
            assertTrue(s.validationErrors.isEmpty())
        }
    }

    // ---------------------------------------------------------
    // HARD CONSTRAINTS
    // ---------------------------------------------------------

    @Test
    fun updateDietaryRestrictions_updatesState() = runTest {
        viewModel = OnboardingViewModel(repository, testUserId)

        val restrictions = listOf(DietaryType.VEGAN, DietaryType.HALAL)
        viewModel.updateDietaryRestrictions(restrictions)

        assertEquals(
            restrictions,
            viewModel.state.value.profileDraft.hardConstraints.dietaryRestrictions
        )
    }

    @Test
    fun updateAllergies_updatesState() = runTest {
        viewModel = OnboardingViewModel(repository, testUserId)

        val allergies = listOf(Allergen.SHELLFISH)
        viewModel.updateAllergies(allergies)

        assertEquals(allergies, viewModel.state.value.profileDraft.hardConstraints.allergies)
    }

    @Test
    fun updateAvoidIngredients_updatesState() = runTest {
        viewModel = OnboardingViewModel(repository, testUserId)

        val avoidList = listOf("Garlic", "Mushroom")
        viewModel.updateAvoidIngredients(avoidList)

        assertEquals(avoidList, viewModel.state.value.profileDraft.hardConstraints.avoidIngredients)
    }

    // ---------------------------------------------------------
    // SOFT PREFERENCES
    // ---------------------------------------------------------

    @Test
    fun updateFavoriteCuisines_updatesState() = runTest {
        viewModel = OnboardingViewModel(repository, testUserId)

        val cuisines = listOf(CuisineType.KOREAN, CuisineType.JAPANESE)
        viewModel.updateFavoriteCuisines(cuisines)

        assertEquals(
            cuisines,
            viewModel.state.value.profileDraft.softPreferences.favoriteCuisines
        )
    }

    @Test
    fun updateSpiceTolerance_updatesState() = runTest {
        viewModel = OnboardingViewModel(repository, testUserId)

        val level = SpiceLevel.HIGH
        viewModel.updateSpiceTolerance(level)

        assertEquals(level, viewModel.state.value.profileDraft.softPreferences.spiceTolerance)
    }

    // ---------------------------------------------------------
    // NAVIGATION
    // ---------------------------------------------------------

    @Test
    fun nextScreen_movesForwardCorrectly() = runTest {
        viewModel = OnboardingViewModel(repository, testUserId)

        viewModel.nextScreen()

        assertEquals(
            OnboardingScreen.DietaryRestrictions,
            viewModel.state.value.currentScreen
        )
    }

    @Test
    fun nextScreen_doesNotMovePastSummary() = runTest {
        viewModel = OnboardingViewModel(repository, testUserId)

        viewModel.navigateToScreen(OnboardingScreen.Summary)
        viewModel.nextScreen()

        assertEquals(OnboardingScreen.Summary, viewModel.state.value.currentScreen)
    }

    @Test
    fun previousScreen_movesBackCorrectly() = runTest {
        viewModel = OnboardingViewModel(repository, testUserId)

        viewModel.navigateToScreen(OnboardingScreen.AvoidIngredients)
        viewModel.previousScreen()

        assertEquals(OnboardingScreen.Allergies, viewModel.state.value.currentScreen)
    }

    @Test
    fun previousScreen_doesNotMoveBeforeWelcome() = runTest {
        viewModel = OnboardingViewModel(repository, testUserId)

        viewModel.previousScreen()

        assertEquals(OnboardingScreen.Welcome, viewModel.state.value.currentScreen)
    }

    // ---------------------------------------------------------
    // SAVING PROFILE
    // ---------------------------------------------------------

    @Test
    fun saveProfile_success_updatesState() = runTest {
        coEvery { repository.saveProfile(any()) } returns Result.success(Unit)

        viewModel = OnboardingViewModel(repository, testUserId)

        viewModel.saveProfile()
        advanceUntilIdle()

        val s = viewModel.state.value
        assertFalse(s.isSaving)
        assertTrue(s.isComplete)
        assertNull(s.saveError)

        coVerify { repository.saveProfile(match { it.isOnboardingComplete }) }
    }

    @Test
    fun saveProfile_failure_setsError() = runTest {
        coEvery { repository.saveProfile(any()) } returns
                Result.failure(Exception("Network down"))

        viewModel = OnboardingViewModel(repository, testUserId)

        viewModel.saveProfile()
        advanceUntilIdle()

        val s = viewModel.state.value
        assertFalse(s.isSaving)
        assertFalse(s.isComplete)
        assertEquals("Network down", s.saveError)
    }

    @Test
    fun saveProfile_unknownError_setsGenericMessage() = runTest {
        coEvery { repository.saveProfile(any()) } returns Result.failure(Exception())

        viewModel = OnboardingViewModel(repository, testUserId)

        viewModel.saveProfile()
        advanceUntilIdle()

        assertEquals(
            "Failed to save profile",
            viewModel.state.value.saveError
        )
    }

    @Test
    fun saveProfile_showsSavingWhileProcessing() = runTest {
        coEvery { repository.saveProfile(any()) } coAnswers {
            delay(200)
            Result.success(Unit)
        }

        viewModel = OnboardingViewModel(repository, testUserId)

        viewModel.saveProfile()
        advanceTimeBy(50)

        assertTrue(viewModel.state.value.isSaving)
    }

    // ---------------------------------------------------------
    // VALIDATION
    // ---------------------------------------------------------

    @Test
    fun validateCurrentScreen_returnsTrueAndClearsErrors() = runTest {
        viewModel = OnboardingViewModel(repository, testUserId)

        val result = viewModel.validateCurrentScreen()

        assertTrue(result)
        assertTrue(viewModel.state.value.validationErrors.isEmpty())
    }

    // ---------------------------------------------------------
    // STATE HELPERS (hard/soft screens, progress, steps)
    // ---------------------------------------------------------

    @Test
    fun stepNumber_and_totalSteps_correctValues() = runTest {
        viewModel = OnboardingViewModel(repository, testUserId)

        val s = viewModel.state.value
        assertEquals(OnboardingScreen.Welcome.stepNumber + 1, s.currentStepNumber)
        assertEquals(OnboardingScreen.TOTAL_STEPS, s.totalSteps)
    }

    @Test
    fun progressPercentage_isCalculatedCorrectly() = runTest {
        viewModel = OnboardingViewModel(repository, testUserId)

        val s = viewModel.state.value
        val expected = s.currentStepNumber.toFloat() / s.totalSteps.toFloat()

        assertEquals(expected, s.getProgressPercentage(), 0.0001f)
    }

    @Test
    fun skipToNextSection_advancesOneStep() = runTest {
        viewModel = OnboardingViewModel(repository, testUserId)

        val start = viewModel.state.value.currentScreen
        viewModel.skipToNextSection()

        assertNotEquals(start, viewModel.state.value.currentScreen)
    }

    @Test
    fun navigateToScreen_setsCorrectScreen() = runTest {
        viewModel = OnboardingViewModel(repository, testUserId)

        viewModel.navigateToScreen(OnboardingScreen.AvoidIngredients)

        assertEquals(OnboardingScreen.AvoidIngredients, viewModel.state.value.currentScreen)
    }

    @Test
    fun saveProfile_setsSavingTrueAndClearsErrorImmediately() = runTest {
        coEvery { repository.saveProfile(any()) } coAnswers {
            delay(1000)   // long delay so completion will NOT run yet
            Result.success(Unit)
        }

        viewModel = OnboardingViewModel(repository, testUserId)

        viewModel.saveProfile()

        // Run only the first queued event (the immediate update)
        advanceTimeBy(1)

        val state = viewModel.state.value
        assertTrue(state.isSaving)
        assertNull(state.saveError)
    }
}
