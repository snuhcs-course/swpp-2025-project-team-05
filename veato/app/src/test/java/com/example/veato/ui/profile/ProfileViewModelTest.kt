package com.example.veato.ui.profile

import com.example.veato.data.model.UserProfile
import com.example.veato.data.repository.UserProfileRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import kotlinx.coroutines.test.advanceUntilIdle

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    @Mock
    private lateinit var mockRepository: UserProfileRepository
    private lateinit var viewModel: ProfileViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        MockitoAnnotations.openMocks(this)

        // Ensure loadProfile() (in init) returns a mock profile so state isn't null
        runBlocking {
            `when`(mockRepository.getProfile("user_123")).thenReturn(
                UserProfile(
                    userId = "user_123",
                    hardConstraints = com.example.veato.data.model.HardConstraints(),
                    softPreferences = com.example.veato.data.model.SoftPreferences(),
                    fullName = "Test User",
                    userName = "testuser",
                    isOnboardingComplete = true
                )
            )
        }

        viewModel = ProfileViewModel(mockRepository, userId = "user_123")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Helper extension to modify private _state safely
    private fun ProfileViewModel.setFakeProfile(profile: UserProfile) {
        val field = ProfileViewModel::class.java.getDeclaredField("_state")
        field.isAccessible = true
        val mutableStateFlow = field.get(this) as kotlinx.coroutines.flow.MutableStateFlow<ProfileState>
        mutableStateFlow.value = mutableStateFlow.value.copy(userProfile = profile)
    }

    @Test
    fun toggleEditing_switchesModes_successfully() {
        val initial = viewModel.state.value.isEditing
        viewModel.toggleEditing()
        val afterToggle = viewModel.state.value.isEditing

        assertThat(afterToggle).isNotEqualTo(initial)
        println("Editing mode toggled: $initial → $afterToggle")
    }

    @Test
    fun updateProfile_callsRepositoryAndUpdatesState_success() = runTest {
        val fakeProfile = UserProfile(
            userId = "user_123",
            hardConstraints = com.example.veato.data.model.HardConstraints(),
            softPreferences = com.example.veato.data.model.SoftPreferences(),
            fullName = "Test User",
            userName = "testuser",
            isOnboardingComplete = true
        )

        // Inject fake profile into state
        viewModel.setFakeProfile(fakeProfile)

        // Mock repository success
        `when`(mockRepository.updateProfile(fakeProfile)).thenReturn(Result.success(Unit))

        viewModel.updateProfile()
        advanceUntilIdle()

        verify(mockRepository, times(1)).updateProfile(fakeProfile)
        assertThat(viewModel.state.value.isBusy).isFalse()
        println("Repository updateProfile() called successfully.")
    }

    @Test
    fun updateProfile_repositoryFailure_setsErrorState() = runTest {
        val fakeProfile = UserProfile(
            userId = "user_123",
            hardConstraints = com.example.veato.data.model.HardConstraints(),
            softPreferences = com.example.veato.data.model.SoftPreferences(),
            fullName = "Test User",
            userName = "testuser",
            isOnboardingComplete = true
        )

        viewModel.setFakeProfile(fakeProfile)

        // Simulate failure
        `when`(mockRepository.updateProfile(fakeProfile))
            .thenReturn(Result.failure(Exception("Network error")))

        viewModel.updateProfile()
        advanceUntilIdle()

        verify(mockRepository, times(1)).updateProfile(fakeProfile)
        assertThat(viewModel.state.value.saveError).contains("Network error")
        println("Repository failure handled gracefully.")
    }
}
