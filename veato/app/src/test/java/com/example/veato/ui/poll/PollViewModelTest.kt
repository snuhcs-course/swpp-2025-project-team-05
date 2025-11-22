package com.example.veato.ui.poll

import app.cash.turbine.test
import com.example.veato.data.model.Candidate
import com.example.veato.data.model.Poll
import com.example.veato.data.model.PollPhase
import com.example.veato.data.repository.PollRepository
import com.example.veato.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PollViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: PollRepository
    private lateinit var viewModel: PollViewModel

    private val basePoll = Poll(
        pollId = "poll1",
        pollTitle = "Test Poll",
        isOpen = true,
        phase = PollPhase.PHASE1,
        candidates = listOf(
            Candidate("A"),
            Candidate("B"),
            Candidate("C")
        )
    )

    @Before
    fun setup() {
        repository = mockk()

        // First call → open poll
        // Second call → closed poll
        coEvery { repository.getPoll("poll1") } returnsMany listOf(
            basePoll,
            basePoll.copy(isOpen = false)
        )

        viewModel = PollViewModel(
            repository = repository,
            userId = "u1",
            pollId = "poll1"
        )
    }

    // -------------------------------------------------
    // modifySelectedIndices
    // -------------------------------------------------
    @Test
    fun modifySelectedIndices_addsAndRemovesCorrectly() = runTest {
        viewModel.modifySelectedIndices(1)
        assertEquals(setOf(1), viewModel.state.value.selectedIndices)

        viewModel.modifySelectedIndices(1)
        assertEquals(emptySet<Int>(), viewModel.state.value.selectedIndices)
    }

    // -------------------------------------------------
    // clearSelectedIndices
    // -------------------------------------------------
    @Test
    fun clearSelectedIndices_clearsState() = runTest {
        viewModel.modifySelectedIndices(0)
        viewModel.modifySelectedIndices(2)

        viewModel.clearSelectedIndices()

        assertEquals(emptySet<Int>(), viewModel.state.value.selectedIndices)
    }

    // -------------------------------------------------
    // setVoted
    // -------------------------------------------------
    @Test
    fun setVoted_updatesState() = runTest {
        viewModel.setVoted(true)
        assertTrue(viewModel.state.value.voted)
    }

    // -------------------------------------------------
    // sendBallot
    // -------------------------------------------------
    @Test
    fun sendBallot_callsRepository_andSetsVoted() = runTest {
        coEvery { repository.sendBallot("poll1", "u1", any()) } returns Unit

        // after vote, loadOnce() calls getPoll() again => closed poll → safe
        coEvery { repository.getPoll("poll1") } returnsMany listOf(
            basePoll,
            basePoll.copy(isOpen = false)
        )

        viewModel.modifySelectedIndices(0)
        viewModel.modifySelectedIndices(2)

        viewModel.sendBallot()

        coVerify { repository.sendBallot("poll1", "u1", listOf(0, 2)) }
        assertTrue(viewModel.state.value.voted)
    }

    // -------------------------------------------------
    // revokeBallot
    // -------------------------------------------------
    @Test
    fun revokeBallot_callsRepository_andClearsSelection() = runTest {
        coEvery { repository.revokeBallot("poll1", "u1") } returns Unit
        coEvery { repository.getPoll("poll1") } returnsMany listOf(
            basePoll,
            basePoll.copy(isOpen = false)
        )

        viewModel.modifySelectedIndices(1)
        viewModel.modifySelectedIndices(2)

        viewModel.revokeBallot()

        coVerify { repository.revokeBallot("poll1", "u1") }
        assertEquals(emptySet<Int>(), viewModel.state.value.selectedIndices)
        assertFalse(viewModel.state.value.voted)
    }

    // -------------------------------------------------
    // submitPhase1Vote
    // -------------------------------------------------
    @Test
    fun submitPhase1Vote_callsRepository() = runTest {
        coEvery { repository.submitPhase1Vote(any(), any(), any()) } returns Unit
        coEvery { repository.getPoll("poll1") } returnsMany listOf(
            basePoll,
            basePoll.copy(isOpen = false)
        )

        viewModel.modifySelectedIndices(0)
        viewModel.setRejectedCandidate(2)

        viewModel.submitPhase1Vote()

        coVerify {
            repository.submitPhase1Vote(
                "poll1",
                listOf(0),
                2
            )
        }
    }

    // -------------------------------------------------
    // submitPhase2Vote
    // -------------------------------------------------
    @Test
    fun submitPhase2Vote_callsRepository() = runTest {
        val phase2Poll = basePoll.copy(phase = PollPhase.PHASE2)

        coEvery { repository.getPoll("poll1") } returnsMany listOf(
            phase2Poll,
            phase2Poll.copy(isOpen = false)
        )

        coEvery { repository.submitPhase2Vote(any(), any()) } returns Unit

        viewModel.modifySelectedIndices(1)

        viewModel.submitPhase2Vote()

        coVerify { repository.submitPhase2Vote("poll1", 1) }
    }
}
