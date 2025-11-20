package com.example.veato.ui.profile

import com.example.veato.data.repository.UserProfileRepository
import com.example.veato.MainDispatcherRule
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelFactoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()   // ← REQUIRED!

    private val repository = mockk<UserProfileRepository>()
    private val factory = ProfileViewModelFactory(repository, "user123")

    @Test
    fun `factory creates ProfileViewModel successfully`() {
        val vm = factory.create(ProfileViewModel::class.java)

        assertNotNull(vm)
        assertTrue(vm is ProfileViewModel)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `factory throws when unknown ViewModel requested`() {
        class DummyVM : androidx.lifecycle.ViewModel()

        factory.create(DummyVM::class.java)
    }
}
