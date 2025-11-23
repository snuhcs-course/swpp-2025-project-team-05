package com.example.veato.ui.profile

import android.net.Uri
import com.example.veato.data.model.*
import com.example.veato.data.repository.UserProfileRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlinx.coroutines.delay
import org.junit.*
import org.junit.Assert.*
import com.example.veato.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val rule = MainDispatcherRule()

    private lateinit var repository: UserProfileRepository
    private lateinit var viewModel: ProfileViewModel

    private val fakeProfile = UserProfile(
        userId = "u1",
        userName = "oldName",
        fullName = "Old Fullname",
        hardConstraints = HardConstraints.EMPTY,
        softPreferences = SoftPreferences.DEFAULT,
        profilePictureUrl = "old.jpg"
    )

    @Before
    fun setup() {
        repository = mockk()
        coEvery { repository.getProfile("u1") } returns fakeProfile
        viewModel = ProfileViewModel(repository, "u1")
    }

    @Test
    fun toggleEditing_entersEditMode() = runTest {
        viewModel.toggleEditing()
        val s = viewModel.state.value
        assertTrue(s.isEditing)
        assertEquals("Old Fullname", s.editedFullName)
        assertEquals("oldName", s.editedUserName)
    }

    @Test
    fun toggleEditing_exitsEditMode() = runTest {
        viewModel.toggleEditing()  // enter edit mode
        viewModel.toggleEditing()  // exit
        val s = viewModel.state.value
        assertFalse(s.isEditing)
        assertNull(s.selectedImageUri)
    }

    @Test
    fun updateEditedFullName_updatesState() = runTest {
        viewModel.updateEditedFullName("New Name")
        assertEquals("New Name", viewModel.state.value.editedFullName)
    }

    @Test
    fun updateEditedUserName_updatesState() = runTest {
        viewModel.updateEditedUserName("newUser")
        assertEquals("newUser", viewModel.state.value.editedUserName)
    }

    @Test
    fun selectImage_updatesState() = runTest {
        val uri = mockk<Uri>()
        viewModel.selectImage(uri)
        assertEquals(uri, viewModel.state.value.selectedImageUri)
    }

    @Test
    fun updateDietaryRestrictions_updatesProfile() = runTest {
        viewModel.updateDietaryRestrictions(listOf(DietaryType.VEGAN))
        assertEquals(
            listOf(DietaryType.VEGAN),
            viewModel.state.value.userProfile?.hardConstraints?.dietaryRestrictions
        )
    }

    @Test
    fun updateAllergies_updatesProfile() = runTest {
        viewModel.updateAllergies(listOf(Allergen.FISH))
        assertEquals(
            listOf(Allergen.FISH),
            viewModel.state.value.userProfile?.hardConstraints?.allergies
        )
    }

    @Test
    fun updateAvoidIngredients_updatesProfile() = runTest {
        viewModel.updateAvoidIngredients(listOf("salt"))
        assertEquals(
            listOf("salt"),
            viewModel.state.value.userProfile?.hardConstraints?.avoidIngredients
        )
    }

    @Test
    fun updateFavoriteCuisines_updatesProfile() = runTest {
        viewModel.updateFavoriteCuisines(listOf(CuisineType.KOREAN))
        assertEquals(
            listOf(CuisineType.KOREAN),
            viewModel.state.value.userProfile?.softPreferences?.favoriteCuisines
        )
    }

    @Test
    fun updateSpiceTolerance_updatesProfile() = runTest {
        viewModel.updateSpiceTolerance(SpiceLevel.HIGH)
        assertEquals(
            SpiceLevel.HIGH,
            viewModel.state.value.userProfile?.softPreferences?.spiceTolerance
        )
    }

    @Test
    fun uploadImage_success() = runTest {
        val uri = mockk<Uri>()
        coEvery { repository.uploadProfileImage("u1", uri) } returns "newPic.jpg"

        val result = viewModel.uploadImage(uri)

        assertTrue(result.isSuccess)
        assertEquals("newPic.jpg", result.getOrNull())
        assertFalse(viewModel.state.value.isUploadingImage)
    }

    @Test
    fun uploadImage_failure() = runTest {
        val uri = mockk<Uri>()
        coEvery { repository.uploadProfileImage("u1", uri) } throws RuntimeException("fail")

        val result = viewModel.uploadImage(uri)

        assertTrue(result.isFailure)
        assertEquals("fail", result.exceptionOrNull()?.message)
        assertFalse(viewModel.state.value.isUploadingImage)
    }

    @Test
    fun updateProfile_emptyFields_setsValidationError() = runTest {
        viewModel.toggleEditing()  // enter edit mode
        viewModel.updateEditedFullName("")
        viewModel.updateEditedUserName("")

        viewModel.updateProfile()

        assertEquals("Name and username cannot be empty", viewModel.state.value.saveError)
    }

    @Test
    fun updateProfile_imageUploadFails_setsError() = runTest {
        viewModel.toggleEditing()

        val uri = mockk<Uri>()
        viewModel.selectImage(uri)

        coEvery { repository.uploadProfileImage("u1", uri) } throws RuntimeException("upload failed")

        viewModel.updateProfile()

        val s = viewModel.state.value
        assertTrue(s.saveError!!.contains("upload failed"))
        assertFalse(s.isBusy)
    }

    @Test
    fun updateProfile_success_updatesState() = runTest {
        viewModel.toggleEditing()

        coEvery { repository.uploadProfileImage(any(), any()) } returns "new.jpg"
        coEvery { repository.updateProfile(any()) } returns Result.success(Unit)
        coEvery { repository.getProfile("u1") } returns fakeProfile.copy(
            fullName = "Updated",
            userName = "Changed"
        )

        viewModel.selectImage(mockk())
        viewModel.updateEditedFullName("Updated")
        viewModel.updateEditedUserName("Changed")

        viewModel.updateProfile()

        val s = viewModel.state.value
        assertFalse(s.isEditing)
        assertEquals("Updated", s.editedFullName)
        assertEquals("Changed", s.editedUserName)
    }

    @Test
    fun updateProfileData_success_callsLoadProfile() = runTest {
        val updated = fakeProfile.copy(fullName = "HELLO")

        coEvery { repository.updateProfile(updated) } returns Result.success(Unit)
        coEvery { repository.getProfile("u1") } returns updated

        viewModel.updateProfileData(updated)

        assertEquals("HELLO", viewModel.state.value.userProfile?.fullName)
    }

    @Test
    fun updateProfileData_failure_setsError() = runTest {
        val updated = fakeProfile.copy(fullName = "HELLO")
        coEvery { repository.updateProfile(updated) } returns Result.failure(Exception("x"))

        viewModel.updateProfileData(updated)

        assertEquals("x", viewModel.state.value.saveError)
    }

    @Test
    fun loadProfile_failure_setsError() = runTest {
        coEvery { repository.getProfile("u1") } throws RuntimeException("load failed")

        viewModel = ProfileViewModel(repository, "u1")

        assertEquals("load failed", viewModel.state.value.saveError)
        assertFalse(viewModel.state.value.isBusy)
    }

    @Test
    fun toggleEditing_entersEditMode_resetsSelectedImage() = runTest {
        viewModel.selectImage(mockk())
        viewModel.toggleEditing()  // Enter edit

        assertNull(viewModel.state.value.selectedImageUri)
    }

    @Test
    fun updateProfile_repositoryFailure_setsError() = runTest {
        viewModel.toggleEditing()
        viewModel.updateEditedFullName("New")
        viewModel.updateEditedUserName("User")

        coEvery { repository.updateProfile(any()) } returns Result.failure(Exception("update failed"))

        viewModel.updateProfile()

        assertEquals("update failed", viewModel.state.value.saveError)
        assertFalse(viewModel.state.value.isBusy)
    }

    @Test
    fun updateProfileData_loadProfileFails_setsError() = runTest {
        val updated = fakeProfile.copy(fullName = "X")

        coEvery { repository.updateProfile(updated) } returns Result.success(Unit)
        coEvery { repository.getProfile("u1") } throws RuntimeException("reload failed")

        viewModel.updateProfileData(updated)

        assertEquals("reload failed", viewModel.state.value.saveError)
    }

    @Test
    fun updateProfile_withoutImageUpload_stillUpdatesProfile() = runTest {
        viewModel.toggleEditing()

        coEvery { repository.updateProfile(any()) } returns Result.success(Unit)
        coEvery { repository.getProfile("u1") } returns fakeProfile.copy(
            fullName = "X",
            userName = "Y"
        )

        viewModel.updateEditedFullName("X")
        viewModel.updateEditedUserName("Y")

        viewModel.updateProfile()

        assertEquals("X", viewModel.state.value.editedFullName)
        assertEquals("Y", viewModel.state.value.editedUserName)
    }

    @Test
    fun updateDietaryRestrictions_noProfile_noCrash() = runTest {
        coEvery { repository.getProfile("u1") } returns null
        viewModel = ProfileViewModel(repository, "u1")

        viewModel.updateDietaryRestrictions(listOf(DietaryType.VEGAN))

        assertNull(viewModel.state.value.userProfile)
    }

    @Test
    fun toggleEditing_exitMode_resetsSaveError() = runTest {
        // enter edit mode
        viewModel.toggleEditing()

        // simulate an error
        viewModel.updateEditedFullName("")
        viewModel.updateEditedUserName("")
        viewModel.updateProfile()
        assertNotNull(viewModel.state.value.saveError)

        // exit edit mode
        viewModel.toggleEditing()

        // saveError MUST be reset
        assertNull(viewModel.state.value.saveError)
    }

    @Test
    fun updateProfile_withoutEnteringEditMode_updatesProfile() = runTest {
        coEvery { repository.updateProfile(any()) } returns Result.success(Unit)
        coEvery { repository.getProfile("u1") } returns fakeProfile.copy(
            fullName = "Z",
            userName = "ZZ"
        )

        viewModel.updateProfile()

        assertEquals("Z", viewModel.state.value.editedFullName)
        assertEquals("ZZ", viewModel.state.value.editedUserName)
    }

    @Test
    fun updateAllergies_noProfile_noCrash() = runTest {
        coEvery { repository.getProfile("u1") } returns null
        viewModel = ProfileViewModel(repository, "u1")

        viewModel.updateAllergies(listOf(Allergen.EGGS))
        assertNull(viewModel.state.value.userProfile)
    }

    @Test
    fun updateAvoidIngredients_noProfile_noCrash() = runTest {
        coEvery { repository.getProfile("u1") } returns null
        viewModel = ProfileViewModel(repository, "u1")

        viewModel.updateAvoidIngredients(listOf("onion"))
        assertNull(viewModel.state.value.userProfile)
    }

    @Test
    fun updateFavoriteCuisines_noProfile_noCrash() = runTest {
        coEvery { repository.getProfile("u1") } returns null
        viewModel = ProfileViewModel(repository, "u1")

        viewModel.updateFavoriteCuisines(listOf(CuisineType.JAPANESE))
        assertNull(viewModel.state.value.userProfile)
    }

    @Test
    fun updateSpiceTolerance_noProfile_noCrash() = runTest {
        coEvery { repository.getProfile("u1") } returns null
        viewModel = ProfileViewModel(repository, "u1")

        viewModel.updateSpiceTolerance(SpiceLevel.LOW)
        assertNull(viewModel.state.value.userProfile)
    }

    @Test
    fun loadProfile_setsBusyTrueAtStart() = runTest {
        val slowRepository = mockk<UserProfileRepository>()
        coEvery { slowRepository.getProfile("u1") } coAnswers {
            delay(50)
            fakeProfile
        }

        val vm = ProfileViewModel(slowRepository, "u1")

        // Immediately after init, before delay finishes
        assertTrue(vm.state.value.isBusy)
    }

}
