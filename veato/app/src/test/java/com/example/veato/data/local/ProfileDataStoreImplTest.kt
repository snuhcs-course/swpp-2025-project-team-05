package com.example.veato.data.local

import android.content.Context
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.example.veato.data.model.UserProfile
import kotlinx.coroutines.flow.first

@RunWith(RobolectricTestRunner::class)
class ProfileDataStoreImplTest {

    private lateinit var context: Context
    private lateinit var dataStore: ProfileLocalDataSource

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        dataStore = ProfileDataStoreImpl(context)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `save and get returns saved profile`() = runTest {
        val profile = UserProfile(
            userId = "u1",
            userName = "testUser",
            fullName = "Tester",
            profilePictureUrl = "",
        )

        dataStore.save(profile)

        val loaded = dataStore.get("u1")

        assert(loaded != null)
        assert(loaded?.userName == "testUser")
    }

    @Test
    fun `delete removes profile`() = runTest {
        val profile = UserProfile(
            userId = "u2",
            userName = "tester2",
            fullName = "Tester Two",
        )

        dataStore.save(profile)
        dataStore.delete("u2")

        val loaded = dataStore.get("u2")
        assert(loaded == null)
    }

    @Test
    fun `exists returns true only when profile saved`() = runTest {
        val profile = UserProfile(
            userId = "u3",
            userName = "tester3",
            fullName = "Tester Three",
        )

        assert(!dataStore.exists("u3"))

        dataStore.save(profile)

        assert(dataStore.exists("u3"))
    }

    @Test
    fun `update modifies timestamp`() = runTest {
        val profile = UserProfile(
            userId = "u4",
            userName = "tester4",
            fullName = "Tester Four",
        )

        dataStore.save(profile)
        val first = dataStore.get("u4")!!

        // slight delay
        Thread.sleep(5)

        dataStore.update(first)
        val second = dataStore.get("u4")!!

        assert(second.updatedAt > first.updatedAt)
    }

    @Test
    fun `getFlow emits values when profile changes`() = runTest {
        val profile = UserProfile(
            userId = "u5",
            userName = "flowTester",
            fullName = "Flow User"
        )

        val flow = dataStore.getFlow("u5")

        // No value yet (should emit null)
        val first = flow.first()
        assert(first == null)

        dataStore.save(profile)

        val second = flow.first()
        assert(second?.userId == "u5")
    }

    @Test
    fun `saving multiple profiles stores independently`() = runTest {
        val p1 = UserProfile("idA", "userA", "User A")
        val p2 = UserProfile("idB", "userB", "User B")

        dataStore.save(p1)
        dataStore.save(p2)

        val loaded1 = dataStore.get("idA")
        val loaded2 = dataStore.get("idB")

        assert(loaded1?.userName == "userA")
        assert(loaded2?.userName == "userB")
    }

    @Test
    fun `update persists modified fields`() = runTest {
        val profile = UserProfile("u6", "oldName", "Old Fullname")

        dataStore.save(profile)

        val updated = profile.copy(
            userName = "newName",
            fullName = "New Fullname"
        )

        dataStore.update(updated)

        val loaded = dataStore.get("u6")
        assert(loaded?.userName == "newName")
        assert(loaded?.fullName == "New Fullname")
    }

    @Test
    fun `exists returns false after delete`() = runTest {
        val profile = UserProfile("u7", "user7", "User Seven")

        dataStore.save(profile)
        assert(dataStore.exists("u7"))

        dataStore.delete("u7")
        assert(!dataStore.exists("u7"))
    }

    @Test
    fun `save overwrites existing profile`() = runTest {
        val p1 = UserProfile("u8", "firstName", "First User")
        val p2 = UserProfile("u8", "secondName", "Second User")

        dataStore.save(p1)
        dataStore.save(p2)

        val loaded = dataStore.get("u8")

        assert(loaded?.userName == "secondName")
        assert(loaded?.fullName == "Second User")
    }

    @Test
    fun `get returns null for missing user`() = runTest {
        val loaded = dataStore.get("unknownUser")
        assert(loaded == null)
    }
}
