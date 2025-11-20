package com.example.veato.ui.onboarding

import androidx.lifecycle.ViewModel
import com.example.veato.data.repository.UserProfileRepository
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class OnboardingViewModelFactoryTest {

    private lateinit var repository: UserProfileRepository
    private val testUserId = "factory_user_123"

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
    }

    @Test
    fun create_withCorrectViewModelClass_returnsOnboardingViewModel() {
        // Arrange
        val factory = OnboardingViewModelFactory(repository, testUserId)

        // Act
        val viewModel = factory.create(OnboardingViewModel::class.java)

        // Assert
        assertTrue(viewModel is OnboardingViewModel)
    }

    @Test
    fun create_withIncorrectViewModelClass_throwsIllegalArgumentException() {
        // Arrange
        val factory = OnboardingViewModelFactory(repository, testUserId)

        // Act + Assert
        assertThrows(IllegalArgumentException::class.java) {
            factory.create(FakeViewModel::class.java)
        }
    }

    // Dummy ViewModel class for incorrect type test
    class FakeViewModel : ViewModel()
}
