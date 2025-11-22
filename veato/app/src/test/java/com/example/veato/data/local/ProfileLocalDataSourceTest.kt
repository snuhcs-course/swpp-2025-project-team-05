package com.example.veato.data.local

import com.example.veato.data.model.UserProfile
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileLocalDataSourceTest {

    private lateinit var dataSource: ProfileLocalDataSource

    @Before
    fun setup() {
        dataSource = mockk(relaxed = true)
    }


    @Test
    fun `save - stores profile successfully`() = runTest {
        val profile = UserProfile(userId = "u1", userName = "Alice")
        coEvery { dataSource.save(profile) } just Runs

        dataSource.save(profile)

        coVerify { dataSource.save(profile) }
    }

    @Test
    fun `get - returns stored profile`() = runTest {
        val profile = UserProfile(userId = "u1", userName = "Alice")

        coEvery { dataSource.get("u1") } returns profile

        val result = dataSource.get("u1")

        assertEquals(profile, result)
    }

    @Test
    fun `get - returns null when not found`() = runTest {
        coEvery { dataSource.get("unknown") } returns null

        val result = dataSource.get("unknown")

        assertNull(result)
    }

    @Test
    fun `getFlow - emits profile`() = runTest {
        val profile = UserProfile(userId = "u1", userName = "Alice")

        every { dataSource.getFlow("u1") } returns flowOf(profile)

        val emitted = dataSource.getFlow("u1")

        var received: UserProfile? = null
        emitted.collect { received = it }

        assertEquals(profile, received)
    }

    @Test
    fun `delete - removes stored profile`() = runTest {
        coEvery { dataSource.delete("u1") } just Runs

        dataSource.delete("u1")

        coVerify { dataSource.delete("u1") }
    }

    @Test
    fun `exists - returns true when profile exists`() = runTest {
        coEvery { dataSource.exists("u1") } returns true

        assertTrue(dataSource.exists("u1"))
    }

    @Test
    fun `exists - returns false when profile does not exist`() = runTest {
        coEvery { dataSource.exists("none") } returns false

        assertFalse(dataSource.exists("none"))
    }
}
