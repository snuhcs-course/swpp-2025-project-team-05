package com.example.veato.ui.poll

import com.example.veato.data.repository.PollRepository
import com.example.veato.MainDispatcherRule
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PollViewModelFactoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<PollRepository>(relaxed = true)

    @Test
    fun `factory creates PollViewModel successfully`() = runTest {
        val factory = PollViewModelFactory(repository, "user123", "pollABC")

        val vm = factory.create(PollViewModel::class.java)

        advanceUntilIdle()   // <-- prevents unfinished coroutine error

        assertNotNull(vm)
        assertTrue(vm is PollViewModel)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `factory throws on unknown ViewModel`() = runTest {
        val factory = PollViewModelFactory(repository, "user123", "pollABC")

        class DummyVM : androidx.lifecycle.ViewModel()

        factory.create(DummyVM::class.java)

        advanceUntilIdle()
    }
}
