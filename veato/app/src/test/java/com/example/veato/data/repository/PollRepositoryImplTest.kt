package com.example.veato.data.repository

import com.example.veato.data.model.Candidate
import com.example.veato.data.model.Poll
import com.example.veato.data.model.PollPhase
import com.example.veato.data.remote.*
import com.google.firebase.Timestamp
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import org.junit.*
import retrofit2.Response
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class PollRepositoryImplTest {

    private lateinit var repository: PollRepositoryImpl
    private lateinit var apiService: PollApiService

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        apiService = mockk(relaxed = true)

        mockkObject(RetrofitClient)
        every { RetrofitClient.pollApiService } returns apiService

        repository = PollRepositoryImpl()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun mockPollResponse(): PollResponse =
        PollResponse(
            pollId = "p123",
            pollTitle = "Test Poll",
            teamId = "team123",
            teamName = "Team X",
            status = "active",
            phase = "phase1",
            remainingSeconds = 100,
            candidates = listOf(
                CandidateResponse("A", ranking = 1, voteCount = 2, isRejected = false),
                CandidateResponse("B", ranking = 2, voteCount = 1, isRejected = false)
            ),
            resultRanking = listOf(RankingResponse(1, "A")),
            lockedInUserCount = 1,
            hasCurrentUserLockedIn = true
        )

    // ----------------------------------------------------------------------
    // startVotingSession()
    // ----------------------------------------------------------------------

    @Test
    fun `startVotingSession - success`() = runTest {
        val fakeResponse = StartSessionResponse(
            pollId = "poll123",
            pollTitle = "Lunch Vote",
            teamName = "Team A",
            duration = 30,
            startedTime = "now",
            candidates = listOf("A", "B")
        )

        coEvery { apiService.startPoll(any()) } returns Response.success(fakeResponse)

        val result = repository.startVotingSession(
            teamId = "team1",
            pollTitle = "Lunch Vote",
            durationMinutes = 30,
            includedMemberIds = listOf("u1", "u2"),
            occasionNote = "test"
        )

        Assert.assertEquals("poll123", result.pollId)
        coVerify { apiService.startPoll(any()) }
    }

    @Test(expected = Exception::class)
    fun `startVotingSession - API returns error`() = runTest {
        val error = Response.error<StartSessionResponse>(
            400,
            ResponseBody.create("application/json".toMediaTypeOrNull(), "Bad Request")
        )

        coEvery { apiService.startPoll(any()) } returns error

        repository.startVotingSession("t1", "Test", 10, listOf("u1"), "")
    }

    @Test(expected = Exception::class)
    fun `startVotingSession - exception thrown`() = runTest {
        coEvery { apiService.startPoll(any()) } throws RuntimeException("Network error")

        repository.startVotingSession("t1", "Test", 10, listOf("u1"), "")
    }

    // ----------------------------------------------------------------------
    // getPoll()
    // ----------------------------------------------------------------------

    @Test
    fun `getPoll - success`() = runTest {
        coEvery { apiService.getPoll(any()) } returns Response.success(mockPollResponse())

        val poll = repository.getPoll("p123")

        Assert.assertEquals("p123", poll.pollId)
        Assert.assertEquals(2, poll.candidates.size)
        Assert.assertEquals(PollPhase.PHASE1, poll.phase)
        coVerify { apiService.getPoll("p123") }
    }

    @Test(expected = Exception::class)
    fun `getPoll - empty body`() = runTest {
        coEvery { apiService.getPoll(any()) } returns Response.success(null)

        repository.getPoll("p123")
    }

    @Test(expected = Exception::class)
    fun `getPoll - error response`() = runTest {
        val error = Response.error<PollResponse>(
            500,
            ResponseBody.create("application/json".toMediaTypeOrNull(), "Server error")
        )

        coEvery { apiService.getPoll(any()) } returns error

        repository.getPoll("p123")
    }

    @Test(expected = Exception::class)
    fun `getPoll - exception thrown`() = runTest {
        coEvery { apiService.getPoll(any()) } throws RuntimeException("Network failure")

        repository.getPoll("p123")
    }

    // ----------------------------------------------------------------------
    // getPoll() tests for missing branches
    // ----------------------------------------------------------------------

    @Test
    fun `getPoll - null phase defaults to PHASE1`() = runTest {
        val r = mockPollResponse().copy(phase = null)

        coEvery { apiService.getPoll("p1") } returns Response.success(r)

        val poll = repository.getPoll("p1")
        Assert.assertEquals(PollPhase.PHASE1, poll.phase)
    }

    @Test
    fun `getPoll - phase2 maps to PHASE2`() = runTest {
        val r = mockPollResponse().copy(phase = "phase2")

        coEvery { apiService.getPoll("p1") } returns Response.success(r)

        val poll = repository.getPoll("p1")
        Assert.assertEquals(PollPhase.PHASE2, poll.phase)
    }

    @Test
    fun `getPoll - rejected candidate parsed correctly`() = runTest {
        val r = mockPollResponse().copy(
            candidates = listOf(
                CandidateResponse("A", ranking = 1, voteCount = 0, isRejected = true)
            )
        )

        coEvery { apiService.getPoll("p1") } returns Response.success(r)

        val poll = repository.getPoll("p1")
        Assert.assertTrue(poll.candidates[0].isRejected)
    }

    @Test
    fun `getPoll - empty candidate list`() = runTest {
        val r = mockPollResponse().copy(candidates = emptyList())

        coEvery { apiService.getPoll("p1") } returns Response.success(r)

        val poll = repository.getPoll("p1")
        Assert.assertTrue(poll.candidates.isEmpty())
    }

    @Test
    fun `getPoll - hasCurrentUserLockedIn false`() = runTest {
        val r = mockPollResponse().copy(hasCurrentUserLockedIn = false)

        coEvery { apiService.getPoll("p1") } returns Response.success(r)

        val poll = repository.getPoll("p1")
        Assert.assertFalse(poll.hasCurrentUserLockedIn)
    }

    // ----------------------------------------------------------------------
    // sendBallot()
    // ----------------------------------------------------------------------

    @Test
    fun `sendBallot - success`() = runTest {
        coEvery { apiService.getPoll("p1") } returns Response.success(mockPollResponse())
        coEvery { apiService.castVote("p1", any()) } returns Response.success(
            VoteResponse(true, listOf("A"), 1)
        )

        repository.sendBallot("p1", "u1", listOf(0))

        coVerify { apiService.castVote("p1", any()) }
    }

    @Test(expected = Exception::class)
    fun `sendBallot - poll not active`() = runTest {
        val inactive = mockPollResponse().copy(status = "closed")
        coEvery { apiService.getPoll("p1") } returns Response.success(inactive)

        repository.sendBallot("p1", "u1", listOf(0))
    }

    @Test(expected = Exception::class)
    fun `sendBallot - API error`() = runTest {
        coEvery { apiService.getPoll("p1") } returns Response.success(mockPollResponse())

        val error = Response.error<VoteResponse>(
            400, ResponseBody.create("application/json".toMediaTypeOrNull(), "Bad request")
        )
        coEvery { apiService.castVote("p1", any()) } returns error

        repository.sendBallot("p1", "u1", listOf(0))
    }

    // test empty choices
    @Test
    fun `sendBallot - empty choice list`() = runTest {
        coEvery { apiService.getPoll("p1") } returns Response.success(mockPollResponse())
        coEvery { apiService.castVote("p1", any()) } returns Response.success(
            VoteResponse(true, emptyList(), 0)
        )

        repository.sendBallot("p1", "u1", emptyList())

        coVerify { apiService.castVote("p1", any()) }
    }

    // ----------------------------------------------------------------------
    // revokeBallot()
    // ----------------------------------------------------------------------

    @Test
    fun `revokeBallot - success`() = runTest {
        coEvery { apiService.castVote("p1", any()) } returns Response.success(
            VoteResponse(true, emptyList(), 0)
        )

        repository.revokeBallot("p1", "u1")

        coVerify { apiService.castVote("p1", any()) }
    }

    @Test(expected = Exception::class)
    fun `revokeBallot - error`() = runTest {
        val error = Response.error<VoteResponse>(
            400,
            ResponseBody.create("application/json".toMediaTypeOrNull(), "Bad request")
        )

        coEvery { apiService.castVote("p1", any()) } returns error

        repository.revokeBallot("p1", "u1")
    }

    // ----------------------------------------------------------------------
    // submitPhase1Vote()
    // ----------------------------------------------------------------------

    @Test
    fun `submitPhase1Vote - success`() = runTest {
        coEvery { apiService.getPoll(any()) } returns Response.success(mockPollResponse())
        coEvery { apiService.castPhase1Vote(any(), any()) } returns Response.success(
            VoteResponse(true, listOf("A"), 1)
        )

        repository.submitPhase1Vote("p1", listOf(0), rejectedIndex = 1)

        coVerify { apiService.castPhase1Vote("p1", any()) }
    }

    @Test(expected = Exception::class)
    fun `submitPhase1Vote - wrong phase`() = runTest {
        val p = mockPollResponse().copy(phase = "phase2")
        coEvery { apiService.getPoll("p1") } returns Response.success(p)

        repository.submitPhase1Vote("p1", listOf(0), null)
    }

    @Test(expected = Exception::class)
    fun `submitPhase1Vote - error`() = runTest {
        coEvery { apiService.getPoll(any()) } returns Response.success(mockPollResponse())

        val error = Response.error<VoteResponse>(
            500,
            ResponseBody.create("application/json".toMediaTypeOrNull(), "Server error")
        )
        coEvery { apiService.castPhase1Vote(any(), any()) } returns error

        repository.submitPhase1Vote("p1", listOf(0), null)
    }

    // missing branch — no rejectedIndex
    @Test
    fun `submitPhase1Vote - no rejectedIndex`() = runTest {
        coEvery { apiService.getPoll(any()) } returns Response.success(mockPollResponse())
        coEvery { apiService.castPhase1Vote(any(), any()) } returns Response.success(
            VoteResponse(true, listOf("A"), 1)
        )

        repository.submitPhase1Vote("p1", listOf(0), null)

        coVerify { apiService.castPhase1Vote("p1", any()) }
    }

    // ----------------------------------------------------------------------
    // submitPhase2Vote()
    // ----------------------------------------------------------------------

    @Test
    fun `submitPhase2Vote - success`() = runTest {
        val p = mockPollResponse().copy(phase = "phase2")
        coEvery { apiService.getPoll("p1") } returns Response.success(p)
        coEvery { apiService.castPhase2Vote(any(), any()) } returns Response.success(
            VoteResponse(true, listOf("A"), 1)
        )

        repository.submitPhase2Vote("p1", 0)

        coVerify { apiService.castPhase2Vote("p1", any()) }
    }

    @Test(expected = Exception::class)
    fun `submitPhase2Vote - wrong phase`() = runTest {
        val p = mockPollResponse().copy(phase = "phase1")
        coEvery { apiService.getPoll("p1") } returns Response.success(p)

        repository.submitPhase2Vote("p1", 0)
    }

    @Test(expected = Exception::class)
    fun `submitPhase2Vote - invalid index`() = runTest {
        val p = mockPollResponse().copy(phase = "phase2")
        coEvery { apiService.getPoll("p1") } returns Response.success(p)

        repository.submitPhase2Vote("p1", 10)
    }

    @Test(expected = Exception::class)
    fun `submitPhase2Vote - error`() = runTest {
        val p = mockPollResponse().copy(phase = "phase2")
        coEvery { apiService.getPoll("p1") } returns Response.success(p)

        val error = Response.error<VoteResponse>(
            400,
            ResponseBody.create("application/json".toMediaTypeOrNull(), "Bad req")
        )
        coEvery { apiService.castPhase2Vote(any(), any()) } returns error

        repository.submitPhase2Vote("p1", 0)
    }

    // missing branch — zero candidates
    @Test(expected = Exception::class)
    fun `submitPhase2Vote - no candidates`() = runTest {
        val p = mockPollResponse().copy(phase = "phase2", candidates = emptyList())

        coEvery { apiService.getPoll("p1") } returns Response.success(p)

        repository.submitPhase2Vote("p1", 0)
    }
}
