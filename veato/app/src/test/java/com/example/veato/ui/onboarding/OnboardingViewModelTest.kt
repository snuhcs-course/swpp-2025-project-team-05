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
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for OnboardingViewModel
 *
 * Following AAA pattern (Arrange, Act, Assert) and test naming convention:
 * methodUnderTest_scenario_expectedResult
 *
 * Test doubles used:
 * - Mock: UserProfileRepository (for verification of interactions)
 * - Stub: Mocked responses (for isolating behavior)
 */
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
        // Arrange: Set up test dispatcher for coroutines
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        // Arrange: Create mock repository
        repository = mockk<UserProfileRepository>(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ============================================================
    // State Initialization Tests
    // ============================================================

    @Test
    fun initialization_withValidUserId_setsInitialStateCorrectly() = runTest {
        // Arrange & Act
        viewModel = OnboardingViewModel(repository, testUserId)

        // Assert
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(OnboardingScreen.Welcome, state.currentScreen)
            assertEquals(testUserId, state.profileDraft.userId)
            assertFalse(state.isSaving)
            assertFalse(state.isComplete)
            assertNull(state.saveError)
            assertTrue(state.validationErrors.isEmpty())
        }
    }

    // ============================================================
    // Update Method Tests (Hard Constraints)
    // ============================================================

    @Test
    fun updateDietaryRestrictions_withValidInput_updatesState() = runTest {
        // Arrange
        viewModel = OnboardingViewModel(repository, testUserId)
        val restrictions = listOf(DietaryType.VEGETARIAN, DietaryType.VEGAN)

        // Act
        viewModel.updateDietaryRestrictions(restrictions)

        // Assert
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(restrictions, state.profileDraft.hardConstraints.dietaryRestrictions)
        }
    }

    @Test
    fun updateDietaryRestrictions_withEmptyList_updatesStateWithEmptyList() = runTest {
        // Arrange
        viewModel = OnboardingViewModel(repository, testUserId)
        val emptyRestrictions = emptyList<DietaryType>()

        // Act
        viewModel.updateDietaryRestrictions(emptyRestrictions)

        // Assert
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.profileDraft.hardConstraints.dietaryRestrictions.isEmpty())
        }
    }

    @Test
    fun updateAllergies_withValidInput_updatesState() = runTest {
        // Arrange
        viewModel = OnboardingViewModel(repository, testUserId)
        val allergies = listOf(Allergen.PEANUTS, Allergen.SHELLFISH)

        // Act
        viewModel.updateAllergies(allergies)

        // Assert
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(allergies, state.profileDraft.hardConstraints.allergies)
        }
    }

    @Test
    fun updateAvoidIngredients_withValidInput_updatesState() = runTest {
        // Arrange
        viewModel = OnboardingViewModel(repository, testUserId)
        val ingredients = listOf("Cilantro", "Mushrooms")

        // Act
        viewModel.updateAvoidIngredients(ingredients)

        // Assert
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(ingredients, state.profileDraft.hardConstraints.avoidIngredients)
        }
    }

    // ============================================================
    // Update Method Tests (Soft Preferences)
    // ============================================================

    @Test
    fun updateFavoriteCuisines_withValidInput_updatesState() = runTest {
        // Arrange
        viewModel = OnboardingViewModel(repository, testUserId)
        val cuisines = listOf(CuisineType.KOREAN, CuisineType.JAPANESE)

        // Act
        viewModel.updateFavoriteCuisines(cuisines)

        // Assert
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(cuisines, state.profileDraft.softPreferences.favoriteCuisines)
        }
    }

    @Test
    fun updateSpiceTolerance_withValidInput_updatesState() = runTest {
        // Arrange
        viewModel = OnboardingViewModel(repository, testUserId)
        val spiceLevel = SpiceLevel.MEDIUM

        // Act
        viewModel.updateSpiceTolerance(spiceLevel)

        // Assert
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(spiceLevel, state.profileDraft.softPreferences.spiceTolerance)
        }
    }

    // NOTE: MealType and PortionSize features not yet implemented in SoftPreferences
    // These tests are commented out until the feature is added

    // @Test
    // fun updateMealTypePreferences_withValidInput_updatesState() = runTest {
    //     // Arrange
    //     viewModel = OnboardingViewModel(repository, testUserId)
    //     val mealTypes = listOf(MealType.RICE_BASED, MealType.NOODLE_BASED)
    //
    //     // Act
    //     viewModel.updateMealTypePreferences(mealTypes)
    //
    //     // Assert
    //     viewModel.state.test {
    //         val state = awaitItem()
    //         assertEquals(mealTypes, state.profileDraft.softPreferences.mealTypePreferences)
    //     }
    // }
    //
    // @Test
    // fun updatePortionPreference_withValidInput_updatesState() = runTest {
    //     // Arrange
    //     viewModel = OnboardingViewModel(repository, testUserId)
    //     val portionSize = PortionSize.LARGE
    //
    //     // Act
    //     viewModel.updatePortionPreference(portionSize)
    //
    //     // Assert
    //     viewModel.state.test {
    //         val state = awaitItem()
    //         assertEquals(portionSize, state.profileDraft.softPreferences.portionPreference)
    //     }
    // }
    //
    // @Test
    // fun updatePortionPreference_withNull_updatesStateWithNull() = runTest {
    //     // Arrange
    //     viewModel = OnboardingViewModel(repository, testUserId)
    //
    //     // Act
    //     viewModel.updatePortionPreference(null)
    //
    //     // Assert
    //     viewModel.state.test {
    //         val state = awaitItem()
    //         assertNull(state.profileDraft.softPreferences.portionPreference)
    //     }
    // }

    // ============================================================
    // Navigation Tests (Boundary Testing)
    // ============================================================

    @Test
    fun nextScreen_fromWelcome_navigatesToDietaryRestrictions() = runTest {
        // Arrange
        viewModel = OnboardingViewModel(repository, testUserId)

        // Act
        viewModel.nextScreen()

        // Assert
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(OnboardingScreen.DietaryRestrictions, state.currentScreen)
        }
    }

    @Test
    fun nextScreen_fromMiddleScreen_navigatesToNextScreen() = runTest {
        // Arrange
        viewModel = OnboardingViewModel(repository, testUserId)
        viewModel.navigateToScreen(OnboardingScreen.Allergies)

        // Act
        viewModel.nextScreen()

        // Assert
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(OnboardingScreen.AvoidIngredients, state.currentScreen)
        }
    }

    @Test
    fun nextScreen_fromSummaryScreen_doesNotNavigate() = runTest {
        // Arrange (Boundary test: last screen)
        viewModel = OnboardingViewModel(repository, testUserId)
        viewModel.navigateToScreen(OnboardingScreen.Summary)

        // Act
        viewModel.nextScreen()

        // Assert
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(OnboardingScreen.Summary, state.currentScreen)
        }
    }

    @Test
    fun previousScreen_fromWelcomeScreen_doesNotNavigate() = runTest {
        // Arrange (Boundary test: first screen)
        viewModel = OnboardingViewModel(repository, testUserId)

        // Act
        viewModel.previousScreen()

        // Assert
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(OnboardingScreen.Welcome, state.currentScreen)
        }
    }

    @Test
    fun nextScreen_whileSaving_doesNotNavigate() = runTest {
        // Arrange: Set state to saving
        viewModel = OnboardingViewModel(repository, testUserId)
        // Simulate slow save operation
        coEvery { repository.saveProfile(any()) } coAnswers {
            kotlinx.coroutines.delay(1000)
            Result.success(Unit)
        }
        viewModel.saveProfile()
        advanceTimeBy(10) // Advance just enough to start saving

        // Act: Try to navigate while saving
        viewModel.nextScreen()
        advanceTimeBy(10)

        // Assert: Should still be on Welcome screen
        assertEquals(OnboardingScreen.Welcome, viewModel.state.value.currentScreen)
        assertTrue(viewModel.state.value.isSaving)
    }

    // ============================================================
    // Save Profile Tests (Using Mocks)
    // ============================================================

    @Test
    fun saveProfile_withValidProfile_success() = runTest {
        // Arrange: Mock successful save
        coEvery { repository.saveProfile(any()) } returns Result.success(Unit)
        viewModel = OnboardingViewModel(repository, testUserId)

        // Act
        viewModel.saveProfile()
        advanceUntilIdle() // Wait for coroutine to complete

        // Assert
        val state = viewModel.state.value
        assertFalse(state.isSaving)
        assertTrue(state.isComplete)
        assertNull(state.saveError)
        // Note: The ViewModel sets state.isComplete = true but doesn't update profileDraft in state

        // Verify repository was called with profile that has isOnboardingComplete = true
        coVerify(exactly = 1) {
            repository.saveProfile(match { it.isOnboardingComplete })
        }
    }

    @Test
    fun saveProfile_withNetworkError_setsErrorState() = runTest {
        // Arrange: Mock failed save with exception
        val errorMessage = "Network connection failed"
        coEvery { repository.saveProfile(any()) } returns
            Result.failure(Exception(errorMessage))
        viewModel = OnboardingViewModel(repository, testUserId)

        // Act
        viewModel.saveProfile()
        advanceUntilIdle()

        // Assert
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isSaving)
            assertFalse(state.isComplete)
            assertNotNull(state.saveError)
            assertEquals(errorMessage, state.saveError)
        }
    }

    @Test
    fun saveProfile_withUnknownError_setsGenericErrorMessage() = runTest {
        // Arrange: Mock failed save without message
        coEvery { repository.saveProfile(any()) } returns
            Result.failure(Exception())
        viewModel = OnboardingViewModel(repository, testUserId)

        // Act
        viewModel.saveProfile()
        advanceUntilIdle()

        // Assert
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isSaving)
            assertFalse(state.isComplete)
            assertEquals("Failed to save profile", state.saveError)
        }
    }

    @Test
    fun saveProfile_setsSavingStateWhileProcessing() = runTest {
        // Arrange: Mock slow save
        coEvery { repository.saveProfile(any()) } coAnswers {
            kotlinx.coroutines.delay(100)
            Result.success(Unit)
        }
        viewModel = OnboardingViewModel(repository, testUserId)

        // Act
        viewModel.saveProfile()
        advanceTimeBy(50) // Advance halfway through save

        // Assert: Check that isSaving is true during the operation
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.isSaving)
            assertNull(state.saveError)
        }
    }

    // ============================================================
    // Validation Tests
    // ============================================================

    @Test
    fun validateCurrentScreen_withNoErrors_returnsTrue() = runTest {
        // Arrange
        viewModel = OnboardingViewModel(repository, testUserId)

        // Act
        val result = viewModel.validateCurrentScreen()

        // Assert
        assertTrue(result)
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.validationErrors.isEmpty())
        }
    }

    // ============================================================
    // Navigation Helper Tests
    // ============================================================

    @Test
    fun navigateToScreen_withSpecificScreen_updatesCurrentScreen() = runTest {
        // Arrange
        viewModel = OnboardingViewModel(repository, testUserId)
        val targetScreen = OnboardingScreen.FavoriteCuisines

        // Act
        viewModel.navigateToScreen(targetScreen)

        // Assert
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(targetScreen, state.currentScreen)
        }
    }

    @Test
    fun skipToNextSection_fromCurrentScreen_navigatesToNextScreen() = runTest {
        // Arrange
        viewModel = OnboardingViewModel(repository, testUserId)
        val initialScreen = OnboardingScreen.Welcome

        // Act
        viewModel.skipToNextSection()

        // Assert
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.currentScreen != initialScreen)
        }
    }
}
