package com.example.veato.data.repository

import android.net.Uri
import com.example.veato.data.local.ProfileLocalDataSource
import com.example.veato.data.model.HardConstraints
import com.example.veato.data.model.SoftPreferences
import com.example.veato.data.model.UserProfile
import com.example.veato.data.remote.ProfileRemoteDataSource
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileRepositoryImplTest {

    private var dispatcher = UnconfinedTestDispatcher()

    private lateinit var local: ProfileLocalDataSource
    private lateinit var remote: ProfileRemoteDataSource
    private lateinit var repo: UserProfileRepositoryImpl

    private val valid = UserProfile(
        userId = "u123",
        userName = "alice",
        fullName = "Alice Wonderland",
        hardConstraints = HardConstraints.EMPTY,
        softPreferences = SoftPreferences.DEFAULT,
        isOnboardingComplete = true
    )

    private val invalid = valid.copy(userId = "")

    @Before
    fun setup() {
        dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)

        local = mockk(relaxed = true)
        remote = mockk(relaxed = true)
        repo = UserProfileRepositoryImpl(local, remote)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }


    @Test
    fun `saveProfile valid → saves locally`() = runTest {
        coEvery { local.save(valid) } just Runs

        val result = repo.saveProfile(valid)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { local.save(valid) }
    }

    @Test
    fun `saveProfile invalid → failure`() = runTest {
        val result = repo.saveProfile(invalid)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { local.save(any()) }
    }

    @Test
    fun `saveProfile exception → failure`() = runTest {
        coEvery { local.save(any()) } throws RuntimeException("boom")

        val result = repo.saveProfile(valid)

        assertTrue(result.isFailure)
    }

    @Test
    fun `getProfile remote returns profile → updates local`() = runTest {
        coEvery { remote.download("u123") } returns valid
        coEvery { local.update(valid) } just Runs

        val p = repo.getProfile("u123")

        assertEquals(valid, p)
        coVerify { local.update(valid) }
    }

    @Test
    fun `getProfile remote returns null → no update`() = runTest {
        coEvery { remote.download("u123") } returns null

        val p = repo.getProfile("u123")

        assertNull(p)
        coVerify(exactly = 0) { local.update(any()) }
    }

    @Test
    fun `getProfile remote throws → returns null`() = runTest {
        coEvery { remote.download(any()) } throws RuntimeException("net error")

        val p = repo.getProfile("u123")

        assertNull(p)
    }

    @Test
    fun `getProfile remote returns profile but local update throws → still returns profile`() = runTest {
        coEvery { remote.download("u123") } returns valid
        coEvery { local.update(valid) } throws RuntimeException("local store error")

        val p = repo.getProfile("u123")

        assertNull(p)
        coVerify { local.update(valid) }
    }

    @Test
    fun `getProfileFlow delegates to local`() = runTest {
        val flow = flowOf(valid)
        every { local.getFlow("u123") } returns flow

        assertEquals(flow, repo.getProfileFlow("u123"))
    }

    @Test
    fun `updateProfile valid → uploads remote then updates local`() = runTest {
        coEvery { remote.upload(valid) } just Runs
        coEvery { local.update(valid) } just Runs

        val result = repo.updateProfile(valid)

        assertTrue(result.isSuccess)
        coVerify { remote.upload(valid) }
        coVerify { local.update(valid) }
    }

    @Test
    fun `updateProfile invalid → failure`() = runTest {
        val result = repo.updateProfile(invalid)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { remote.upload(any()) }
        coVerify(exactly = 0) { local.update(any()) }
    }

    @Test
    fun `updateProfile remote upload throws → failure`() = runTest {
        coEvery { remote.upload(any()) } throws RuntimeException("upload error")

        val result = repo.updateProfile(valid)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { local.update(any()) }
    }

    @Test
    fun `updateProfile local update throws → failure`() = runTest {
        coEvery { remote.upload(any()) } just Runs
        coEvery { local.update(any()) } throws RuntimeException("update err")

        val result = repo.updateProfile(valid)

        assertTrue(result.isFailure)
    }

    @Test
    fun `deleteProfile success → local delete`() = runTest {
        coEvery { local.delete("u123") } just Runs

        val result = repo.deleteProfile("u123")

        assertTrue(result.isSuccess)
        coVerify { local.delete("u123") }
    }

    @Test
    fun `deleteProfile local throws → failure`() = runTest {
        coEvery { local.delete(any()) } throws RuntimeException("del error")

        val result = repo.deleteProfile("u123")

        assertTrue(result.isFailure)
    }

    @Test
    fun `isOnboardingComplete true when profile exists`() = runTest {
        coEvery { local.get("u123") } returns valid

        assertTrue(repo.isOnboardingComplete("u123"))
    }

    @Test
    fun `isOnboardingComplete false when profile incomplete`() = runTest {
        coEvery { local.get("u123") } returns valid.copy(isOnboardingComplete = false)

        assertFalse(repo.isOnboardingComplete("u123"))
    }

    @Test
    fun `isOnboardingComplete false when profile null`() = runTest {
        coEvery { local.get("u123") } returns null

        assertFalse(repo.isOnboardingComplete("u123"))
    }

    @Test
    fun `isOnboardingComplete exception → false`() = runTest {
        coEvery { local.get(any()) } throws RuntimeException("fail")

        assertFalse(repo.isOnboardingComplete("u123"))
    }

    @Test
    fun `uploadProfileImage success → returns URL`() = runTest {
        val uri = mockk<Uri>()
        coEvery { remote.uploadProfileImage("u123", uri) } returns "http://x.com/p.png"

        val result = repo.uploadProfileImage("u123", uri)

        assertEquals("http://x.com/p.png", result)
    }

    @Test(expected = RuntimeException::class)
    fun `uploadProfileImage remote throws → propagates`() = runTest {
        val uri = mockk<Uri>()
        coEvery { remote.uploadProfileImage(any(), any()) } throws RuntimeException("boom")

        repo.uploadProfileImage("u123", uri)
    }
}
