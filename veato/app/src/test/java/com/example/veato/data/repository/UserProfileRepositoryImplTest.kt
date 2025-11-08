package com.example.veato.data.repository

import com.example.veato.data.local.ProfileLocalDataSource
import com.example.veato.data.model.Allergen
import com.example.veato.data.model.CuisineType
import com.example.veato.data.model.DietaryType
import com.example.veato.data.model.HardConstraints
import com.example.veato.data.model.MealType
import com.example.veato.data.model.PortionSize
import com.example.veato.data.model.SoftPreferences
import com.example.veato.data.model.SpiceLevel
import com.example.veato.data.model.UserProfile
import com.example.veato.data.remote.ProfileRemoteDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(JUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileRepositoryImplTest {
    private lateinit var localDataSource: ProfileLocalDataSource
    private lateinit var remoteDataSource: ProfileRemoteDataSource
    private lateinit var repository: UserProfileRepositoryImpl

    @Before
    fun setup() {
        localDataSource = mock()
        remoteDataSource = mock()
        repository = UserProfileRepositoryImpl(localDataSource, remoteDataSource)
    }

    // create dummy profile data
    private fun fakeUserProfile(): UserProfile {
        return UserProfile(
            userId = "uid_123",
            fullName = "John Doe",
            userName = "john123",
            hardConstraints = HardConstraints(
                dietaryRestrictions = listOf(DietaryType.VEGETARIAN),
                allergies = listOf(Allergen.PEANUTS),
                avoidIngredients = listOf("Shellfish")
            ),
            softPreferences = SoftPreferences(
                favoriteCuisines = listOf(CuisineType.KOREAN),
                spiceTolerance = SpiceLevel.MEDIUM, // existing enum
                mealTypePreferences = listOf(MealType.RICE_BASED),
                portionPreference = PortionSize.MEDIUM
            ),
            isOnboardingComplete = true
        )
    }

    // local save should validate & call only localDataSource.save()
    @Test
    fun saveProfile_valid_callsLocalSaveOnly() = runTest {
        val dummyProfile = fakeUserProfile()

        repository.saveProfile(dummyProfile)

        verify(localDataSource, times(1)).save(dummyProfile)
        verify(remoteDataSource, never()).upload(any())
    }

    // updateProfile should upload to remote and update local
    @Test
    fun updateProfile_valid_callsRemoteUploadAndLocalUpdate() = runTest {
        val dummyProfile = fakeUserProfile()

        repository.updateProfile(dummyProfile)

        verify(remoteDataSource).upload(dummyProfile)
        verify(localDataSource).update(dummyProfile)
    }

    // invalid profile should return failure without touching data sources
    @Test
    fun saveProfile_invalid_returnsFailure() = runTest {
        val invalidProfile = spy(fakeUserProfile().copy(fullName = ""))

        // Force the validation to fail (simulate invalid data)
        whenever(invalidProfile.validate()).thenReturn(false)

        val result = repository.saveProfile(invalidProfile)

        assert(result.isFailure) { "Expected failure when saving invalid profile" }

        verify(localDataSource, never()).save(any())
        verify(remoteDataSource, never()).upload(any())

        println("Invalid profile correctly returned Result.failure.")
    }


    // getProfile downloads from remote and updates local
    @Test
    fun getProfile_remoteAvailable_updatesLocalAndReturnsProfile() = runTest {
        val remoteProfile = fakeUserProfile().copy(fullName = "Remote User")
        whenever(remoteDataSource.download(any())).thenReturn(remoteProfile)

        val result = repository.getProfile("uid_123")

        verify(remoteDataSource).download("uid_123")
        verify(localDataSource).update(remoteProfile)
        assert(result?.fullName == "Remote User")
    }

    // remote error should fallback and return null
    @Test
    fun getProfile_remoteThrows_returnsNull() = runTest {
        whenever(remoteDataSource.download(any())).thenThrow(RuntimeException("Network error"))

        val result = repository.getProfile("uid_123")

        assert(result == null)
        verify(localDataSource, never()).update(any())
    }
}