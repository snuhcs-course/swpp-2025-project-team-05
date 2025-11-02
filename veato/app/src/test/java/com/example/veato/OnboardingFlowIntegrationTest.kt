package com.example.veato

import com.example.veato.data.model.*
import com.example.veato.data.repository.UserProfileRepository
import com.example.veato.ui.onboarding.OnboardingViewModel
import com.example.veato.ui.onboarding.OnboardingScreen
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingFlowIntegrationTest {
    private lateinit var repository: UserProfileRepository
    private lateinit var viewModel: OnboardingViewModel
    private val fakeUserId = "user_123"

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // mock repository
        repository = mock()

        // Use runBlocking to call suspend functions directly in mocks
        runBlocking {
            `when`(repository.saveProfile(org.mockito.kotlin.any())).thenReturn(Result.success(Unit))
            `when`(repository.getProfile(fakeUserId)).thenReturn(UserProfile.createNew(fakeUserId))
            `when`(repository.isOnboardingComplete(fakeUserId)).thenReturn(false)
        }

        viewModel = OnboardingViewModel(repository, fakeUserId)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onboardingFlow_updatesStateCorrectly() = runTest {
        val initial = viewModel.state.value
        assertThat(initial.profileDraft.userId).isEqualTo(fakeUserId)

        viewModel.updateDietaryRestrictions(listOf(DietaryType.VEGETARIAN))
        viewModel.updateFavoriteCuisines(listOf(CuisineType.KOREAN))
        viewModel.updateSpiceTolerance(SpiceLevel.MEDIUM)

        val updated = viewModel.state.value
        assertThat(updated.profileDraft.hardConstraints.dietaryRestrictions)
            .containsExactly(DietaryType.VEGETARIAN)
        assertThat(updated.profileDraft.softPreferences.favoriteCuisines)
            .containsExactly(CuisineType.KOREAN)
        assertThat(updated.profileDraft.softPreferences.spiceTolerance)
            .isEqualTo(SpiceLevel.MEDIUM)

        val current = updated.currentScreen
        viewModel.nextScreen()
        val afterNext = viewModel.state.value
        assertThat(afterNext.currentScreen).isNotEqualTo(current)
    }

    @Test
    fun saveProfile_callsRepositoryAndUpdatesState() = runTest {
        viewModel.updateDietaryRestrictions(listOf(DietaryType.HALAL))
        viewModel.updateFavoriteCuisines(listOf(CuisineType.JAPANESE))

        // Trigger the coroutine in viewModelScope
        viewModel.saveProfile()

        // Advance the test dispatcher so that launched coroutines finish
        testDispatcher.scheduler.advanceUntilIdle()

        // Now verification will work
        verify(repository).saveProfile(org.mockito.kotlin.any())

        val state = viewModel.state.value
        assertThat(state.isComplete).isTrue()
        assertThat(state.saveError).isNull()
    }

    @Test
    fun saveProfile_failure_setsErrorState() = runTest {
        runBlocking {
            org.mockito.Mockito.`when`(
                repository.saveProfile(org.mockito.kotlin.any())
            ).thenReturn(Result.failure(Exception("Network error")))
        }

        viewModel.saveProfile()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.isSaving).isFalse()
        assertThat(state.isComplete).isFalse()
        assertThat(state.saveError).isEqualTo("Network error")
    }

    @Test
    fun updateSoftPreferences_updatesState() = runTest {
        viewModel.updateSpiceTolerance(SpiceLevel.HIGH)
        viewModel.updateMealTypePreferences(listOf(MealType.NOODLE_BASED))
        viewModel.updatePortionPreference(PortionSize.LARGE)

        val updated = viewModel.state.value.profileDraft.softPreferences
        assertThat(updated.spiceTolerance).isEqualTo(SpiceLevel.HIGH)
        assertThat(updated.mealTypePreferences).containsExactly(MealType.NOODLE_BASED)
        assertThat(updated.portionPreference).isEqualTo(PortionSize.LARGE)
    }

    @Test
    fun validateCurrentScreen_returnsTrueWithoutErrors() = runTest {
        val result = viewModel.validateCurrentScreen()
        val updated = viewModel.state.value

        assertThat(result).isTrue()
        assertThat(updated.validationErrors).isEmpty()
    }

    @Test
    fun skipToNextSection_behavesLikeNextScreen() = runTest {
        val before = viewModel.state.value.currentScreen
        viewModel.skipToNextSection()
        val after = viewModel.state.value.currentScreen
        assertThat(after).isNotEqualTo(before)
    }

    @Test
    fun navigation_nextScreen_worksWithoutCrash() = runTest {
        // Manually set an initial screen
        viewModel.navigateToScreen(OnboardingScreen.Welcome)
        testDispatcher.scheduler.advanceUntilIdle()

        val initial = viewModel.state.value.currentScreen
        assertThat(initial).isNotNull()

        // Test that nextScreen() changes the screen safely
        viewModel.nextScreen()
        testDispatcher.scheduler.advanceUntilIdle()

        val afterNext = viewModel.state.value.currentScreen
        assertThat(afterNext).isNotEqualTo(initial)
    }
}
