package com.example.veato

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import com.example.veato.data.model.Poll
import com.example.veato.data.model.PollPhase
import com.example.veato.data.model.Candidate
import com.example.veato.ui.poll.PollScreenState
import com.example.veato.ui.poll.PollViewModel
import com.example.veato.PollSessionScreen
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider


@RunWith(RobolectricTestRunner::class)
class VoteSessionActivityTest {

    @get:Rule
    val composeRule = createComposeRule()

    // Provide fake owner so viewModel() can resolve
    @Composable
    fun TestWrapper(content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalViewModelStoreOwner provides object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            },
            content = content
        )
    }

    @Before
    fun setupFirebase() {
        mockkStatic(FirebaseAuth::class)
        val mockAuth = mockk<FirebaseAuth>(relaxed = true)
        every { FirebaseAuth.getInstance() } returns mockAuth

        val mockUser = mockk<FirebaseUser>(relaxed = true)
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns "test_user"
    }

    // Helpers
    // Create fake PollViewModel with fixed state
    private fun fakeViewModel(state: PollScreenState): PollViewModel {
        val vm = mockk<PollViewModel>(relaxed = true)
        every { vm.state } returns MutableStateFlow(state)
        return vm
    }

    // Correctly mock reified viewModel()
    private fun inject(fakeVM: PollViewModel) {
        mockkConstructor(ViewModelProvider::class)

        every {
            anyConstructed<ViewModelProvider>().get(PollViewModel::class.java)
        } returns fakeVM
    }


    @Test
    fun pollSessionScreen_showsLoading() {
        val vm = fakeViewModel(PollScreenState(isBusy = true, poll = null))
        inject(vm)

        composeRule.setContent { TestWrapper { PollSessionScreen("poll1") } }

        composeRule.onNodeWithText("loading data...", substring = true).assertExists()
    }

    @Test
    fun pollSessionScreen_errorOnNullPoll() {
        val vm = fakeViewModel(PollScreenState(isBusy = false, poll = null))
        inject(vm)

        composeRule.setContent { TestWrapper { PollSessionScreen("poll1") } }

        composeRule.onNodeWithText("Error: Poll not found").assertExists()
    }

    @Test
    fun pollSessionScreen_displaysPhase1() {
        val poll = Poll(
            pollId = "p1",
            teamId = "t1",
            teamName = "Team A",
            pollTitle = "Dinner Vote",
            phase = PollPhase.PHASE1,
            duration = 30,
            candidates = listOf(
                Candidate("Menu A"),
                Candidate("Menu B")
            )
        )

        val vm = fakeViewModel(PollScreenState(isBusy = false, poll = poll))
        inject(vm)

        composeRule.setContent { TestWrapper { PollSessionScreen("poll1") } }

        composeRule.onNodeWithText("Phase 1: Voting", substring = true).assertExists()
        composeRule.onNodeWithText("Vote for menus you like.", substring = true).assertExists()
    }

    @Test
    fun pollSessionScreen_displaysPhase2() {
        val poll = Poll(
            pollId = "p1",
            teamId = "t1",
            teamName = "Team A",
            pollTitle = "Lunch Vote",
            phase = PollPhase.PHASE2,
            duration = 30,
            candidates = listOf(
                Candidate("Menu A", voteCount = 3),
                Candidate("Menu B", voteCount = 5),
                Candidate("Menu C", voteCount = 1)
            )
        )

        val vm = fakeViewModel(PollScreenState(isBusy = false, poll = poll))
        inject(vm)

        composeRule.setContent { TestWrapper { PollSessionScreen("poll1") } }

        composeRule.onNodeWithText("Phase 2: Final Vote", substring = true).assertExists()
        composeRule.onNodeWithText("Final vote: Choose your top pick", substring = true).assertExists()
    }

    @Test
    fun pollSessionScreen_displaysResults() {
        val poll = Poll(
            pollId = "p1",
            teamId = "t1",
            teamName = "Team A",
            pollTitle = "Supper Vote",
            phase = PollPhase.CLOSED,
            duration = 0,
            candidates = emptyList(),
            results = listOf(
                Candidate("Menu A", voteCount = 7),
                Candidate("Menu B", voteCount = 4)
            )
        )

        val vm = fakeViewModel(PollScreenState(isBusy = false, poll = poll))
        inject(vm)

        composeRule.setContent { TestWrapper { PollSessionScreen("poll1") } }

        composeRule.onNodeWithText("Winner", substring = true).assertExists()
        composeRule.onNodeWithText("Complete Results").assertExists()
        composeRule.onNodeWithText("Back to Team").assertExists()
    }

    @Test
    fun pollSessionScreen_fallbackUserIdWhenFirebaseReturnsNull() {
        // Firebase returns null user
        mockkStatic(FirebaseAuth::class)
        val mockAuth = mockk<FirebaseAuth>(relaxed = true)
        every { FirebaseAuth.getInstance() } returns mockAuth
        every { mockAuth.currentUser } returns null

        val poll = Poll(
            pollId = "p1",
            teamId = "t1",
            teamName = "Team",
            pollTitle = "Vote",
            phase = PollPhase.PHASE1,
            candidates = listOf(Candidate("Item"))
        )

        val vm = fakeViewModel(PollScreenState(isBusy = false, poll = poll))
        inject(vm)

        composeRule.setContent { TestWrapper { PollSessionScreen("poll1") } }

        composeRule.onNodeWithText("Phase 1", substring = true).assertExists()
    }

    @Test
    fun pollSessionScreen_phase1_userAlreadyVoted_showsLockedMessage() {
        val poll = Poll(
            pollId = "p1",
            teamId = "t1",
            teamName = "Team",
            pollTitle = "Vote",
            phase = PollPhase.PHASE1,
            candidates = listOf(Candidate("Menu A"))
        )

        val state = PollScreenState(
            poll = poll,
            isBusy = false,
            voted = true
        )

        val vm = fakeViewModel(state)
        inject(vm)

        composeRule.setContent { TestWrapper { PollSessionScreen("poll1") } }

        composeRule.onNodeWithText("Vote locked in", substring = true).assertExists()
    }

    @Test
    fun pollSessionScreen_phase2_userLockedIn_showsWaitingCard() {
        val poll = Poll(
            pollId = "p1",
            teamId = "team",
            teamName = "Team",
            pollTitle = "Vote",
            phase = PollPhase.PHASE2,
            hasCurrentUserLockedIn = true,
            candidates = listOf(
                Candidate("A", voteCount = 1),
                Candidate("B", voteCount = 2),
                Candidate("C", voteCount = 3)
            )
        )

        val state = PollScreenState(
            poll = poll,
            voted = false,   // lockedIn by poll instead of state
            isBusy = false
        )

        val vm = fakeViewModel(state)
        inject(vm)

        composeRule.setContent { TestWrapper { PollSessionScreen("poll1") } }

        composeRule.onNodeWithText("Final vote locked in", substring = true).assertExists()
    }

    @Test
    fun pollSessionScreen_phase2_lockInDisabledUntilSelection() {
        val poll = Poll(
            pollId = "p1",
            teamId = "team",
            teamName = "Team",
            pollTitle = "Vote",
            phase = PollPhase.PHASE2,
            candidates = listOf(
                Candidate("A", voteCount = 1),
                Candidate("B", voteCount = 2),
                Candidate("C", voteCount = 3)
            )
        )

        val vm = fakeViewModel(PollScreenState(isBusy = false, poll = poll))
        inject(vm)

        composeRule.setContent { TestWrapper { PollSessionScreen("poll1") } }

        composeRule
            .onNodeWithText("Select a menu to lock in")
            .assertExists()
    }

    @Test
    fun pollSessionScreen_phase1_reject_callsViewModel() {
        val poll = Poll(
            pollId = "p1",
            teamId = "t1",
            teamName = "Team",
            pollTitle = "Vote",
            phase = PollPhase.PHASE1,
            candidates = listOf(Candidate("Menu A"))
        )

        val vm = fakeViewModel(PollScreenState(isBusy = false, poll = poll))
        inject(vm)

        composeRule.setContent { TestWrapper { PollSessionScreen("poll1") } }

        composeRule.onNodeWithContentDescription("Reject").performClick()
        composeRule.onNodeWithText("Reject").performClick()

        verify { vm.setRejectedCandidate(0) }
    }

    @Test
    fun pollSessionScreen_phase1_lockIn_callsSubmitPhase1Vote() {
        val poll = Poll(
            pollId = "p1",
            teamId = "t1",
            teamName = "Team",
            pollTitle = "Vote",
            phase = PollPhase.PHASE1,
            candidates = listOf(Candidate("Menu A"))
        )

        val state = PollScreenState(
            poll = poll,
            isBusy = false,
            selectedIndices = setOf(0)
        )

        val vm = fakeViewModel(state)
        inject(vm)

        composeRule.setContent { TestWrapper { PollSessionScreen("poll1") } }

        composeRule.onNodeWithText("1 selected", substring = true).performClick()

        verify { vm.submitPhase1Vote() }
    }

    @Test
    fun pollSessionScreen_phase2_select_callsModifyAndClear() {
        val poll = Poll(
            pollId = "p1",
            teamId = "team",
            teamName = "Team",
            pollTitle = "Vote",
            phase = PollPhase.PHASE2,
            candidates = listOf(
                Candidate("A", voteCount = 1),
                Candidate("B", voteCount = 2),
                Candidate("C", voteCount = 3)
            )
        )

        val state = PollScreenState(isBusy = false, poll = poll)

        val vm = fakeViewModel(state)
        inject(vm)

        composeRule.setContent { TestWrapper { PollSessionScreen("poll1") } }

        composeRule.onNodeWithText("A").performClick()

        verify { vm.clearSelectedIndices() }
        verify { vm.modifySelectedIndices(0) }
    }

    @Test
    fun pollSessionScreen_phase2_lockIn_callsSubmitPhase2Vote() {
        val poll = Poll(
            pollId = "p1",
            teamId = "team",
            teamName = "Team",
            pollTitle = "Vote",
            phase = PollPhase.PHASE2,
            candidates = listOf(Candidate("A"))
        )

        val state = PollScreenState(
            poll = poll,
            isBusy = false,
            selectedIndices = setOf(0)
        )

        val vm = fakeViewModel(state)
        inject(vm)

        composeRule.setContent { TestWrapper { PollSessionScreen("poll1") } }

        composeRule.onNodeWithText("Lock In Final Vote").performClick()

        verify { vm.submitPhase2Vote() }
    }

    @Test
    fun phase1_checkboxDisabledWhenLockedByPoll() {
        val poll = Poll(
            pollId = "p1",
            teamId = "team",
            teamName = "Team",
            pollTitle = "Vote",
            phase = PollPhase.PHASE1,
            hasCurrentUserLockedIn = true,
            candidates = listOf(Candidate("Menu A"))
        )

        val vm = fakeViewModel(PollScreenState(isBusy = false, poll = poll))
        inject(vm)

        composeRule.setContent { TestWrapper { PollSessionScreen("poll1") } }

        composeRule.onNode(hasText("Menu A")).performClick()

        verify(exactly = 0) { vm.modifySelectedIndices(any()) }
    }

    @Test
    fun phase1_rejectDisabledAfterAlreadyRejected() {
        val poll = Poll(
            pollId = "p1",
            teamId = "team",
            teamName = "Team",
            pollTitle = "Vote",
            phase = PollPhase.PHASE1,
            candidates = listOf(
                Candidate("A"),
                Candidate("B")
            )
        )

        val state = PollScreenState(
            poll = poll,
            isBusy = false,
            rejectionUsed = true,
            rejectedCandidateIndex = 0
        )

        val vm = fakeViewModel(state)
        inject(vm)

        composeRule.setContent { TestWrapper { PollSessionScreen("poll1") } }

        // Click reject for candidate 1 → should not trigger dialog
        composeRule.onAllNodesWithContentDescription("Reject")[1].performClick()

        composeRule.onNodeWithText("Reject Menu?").assertDoesNotExist()
    }
}
