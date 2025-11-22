package com.example.veato

import android.content.Intent
import android.os.Looper
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.FrameLayout

import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowToast


@RunWith(RobolectricTestRunner::class)
class MyTeamsActivityTest {

    private lateinit var mockAuth: FirebaseAuth
    private lateinit var mockUser: FirebaseUser
    private lateinit var mockFirestore: FirebaseFirestore
    private lateinit var mockCollection: CollectionReference
    private lateinit var mockQuery: Query
    private lateinit var mockRegistration: ListenerRegistration

    @Before
    fun setup() {
        // Mock FirebaseAuth
        mockkStatic(FirebaseAuth::class)
        mockAuth = mockk(relaxed = true)
        mockUser = mockk(relaxed = true)

        every { FirebaseAuth.getInstance() } returns mockAuth
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns "fakeUser123"

        // Mock Firestore
        mockkStatic(FirebaseFirestore::class)
        mockFirestore = mockk(relaxed = true)
        mockCollection = mockk(relaxed = true)
        mockQuery = mockk(relaxed = true)
        mockRegistration = mockk(relaxed = true)

        every { FirebaseFirestore.getInstance() } returns mockFirestore
        every { mockFirestore.collection("teams") } returns mockCollection
        every { mockCollection.whereArrayContains("members", any()) } returns mockQuery
        every { mockQuery.orderBy(any<String>(), any()) } returns mockQuery
        every { mockQuery.addSnapshotListener(any()) } returns mockRegistration

        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun activityLaunches_andRecyclerViewExists() {
        ActivityScenario.launch(MyTeamsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->

                val root = activity.findViewById<ViewGroup>(android.R.id.content)
                assertNotNull(root)

                val rv = findViewByType(root, RecyclerView::class.java)
                assertNotNull("RecyclerView should exist in Compose AndroidView", rv)

                assertTrue(rv!!.adapter is TeamAdapter)
            }
        }
    }

    @Test
    fun clickingTeamItem_opensTeamDetailActivity() {
        // Prepare fake Firestore snapshot
        val fakeSnapshot = mockk<QuerySnapshot>(relaxed = true)
        val fakeDoc = mockk<QueryDocumentSnapshot>(relaxed = true)

        every { fakeSnapshot.iterator() } returns mutableListOf(fakeDoc).iterator()
        every { fakeDoc.id } returns "team001"
        every { fakeDoc.toObject(Team::class.java) } returns Team(
            id = "team001",
            name = "Test Team",
            leaderId = "fakeUser123",
            members = listOf("fakeUser123")
        )

        every {
            mockQuery.addSnapshotListener(any())
        } answers {
            val listener = arg<EventListener<QuerySnapshot>>(0)
            listener.onEvent(fakeSnapshot, null)
            mockRegistration
        }

        ActivityScenario.launch(MyTeamsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val root = activity.findViewById<ViewGroup>(android.R.id.content)
                val rv = findViewByType(root, RecyclerView::class.java)
                assertNotNull("RecyclerView should exist", rv)

                val holder = rv!!.findViewHolderForAdapterPosition(0)
                assertNotNull("ViewHolder should exist at position 0", holder)

                holder!!.itemView.performClick()

                val shadow = Shadows.shadowOf(activity)
                val nextIntent: Intent = shadow.nextStartedActivity

                assertEquals(
                    TeamDetailActivity::class.java.name,
                    nextIntent.component?.className
                )

                assertEquals("team001", nextIntent.getStringExtra("teamId"))
                assertEquals("Test Team", nextIntent.getStringExtra("teamName"))
            }
        }
    }

    @Test
    fun loadTeams_whenFirestoreFails_showsErrorToast() {
        every {
            mockQuery.addSnapshotListener(any())
        } answers {
            val listener = arg<EventListener<QuerySnapshot>>(0)
            listener.onEvent(null, FirebaseFirestoreException("boom!", FirebaseFirestoreException.Code.CANCELLED))
            mockRegistration
        }

        ActivityScenario.launch(MyTeamsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val shadowToast = ShadowToast.getLatestToast()

                assertNotNull(shadowToast)
            }
        }
    }

    @Test
    fun teamAdapter_showsActivePollIndicator() {
        val team = Team(
            id = "123",
            name = "Test Team",
            members = listOf("a", "b"),
            currentlyOpenPoll = "poll123"
        )

        val parent = FrameLayout(RuntimeEnvironment.getApplication())
        val adapter = TeamAdapter(listOf(team)) {}

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        assertEquals(View.VISIBLE, holder.tvActivePoll.visibility)
        assertEquals("● ACTIVE POLL", holder.tvActivePoll.text)
    }

    @Test
    fun teamAdapter_hidesActivePollWhenNull() {
        val team = Team(
            id = "123",
            name = "Test Team",
            members = listOf("a", "b"),
            currentlyOpenPoll = null
        )

        val parent = FrameLayout(RuntimeEnvironment.getApplication())
        val adapter = TeamAdapter(listOf(team)) {}

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        assertEquals(View.GONE, holder.tvActivePoll.visibility)
    }

    @Test
    fun loadTeams_withEmptySnapshot_resultsInEmptyList() {
        val fakeSnapshot = mockk<QuerySnapshot>(relaxed = true)

        every { fakeSnapshot.iterator() } returns mutableListOf<QueryDocumentSnapshot>().iterator()

        every {
            mockQuery.addSnapshotListener(any())
        } answers {
            val listener = arg<EventListener<QuerySnapshot>>(0)
            listener.onEvent(fakeSnapshot, null)
            mockRegistration
        }

        ActivityScenario.launch(MyTeamsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->

                val root = activity.findViewById<ViewGroup>(android.R.id.content)
                val rv = findViewByType(root, RecyclerView::class.java)

                assertNotNull(rv)
                assertEquals(0, rv!!.adapter!!.itemCount)
            }
        }
    }

    @Test
    fun loadTeams_multipleSnapshots_updatesList() {
        val doc1 = mockk<QueryDocumentSnapshot>(relaxed = true)
        val doc2 = mockk<QueryDocumentSnapshot>(relaxed = true)

        every { doc1.id } returns "id1"
        every { doc1.toObject(Team::class.java) } returns Team(id = "id1", name = "Team A")

        every { doc2.id } returns "id2"
        every { doc2.toObject(Team::class.java) } returns Team(id = "id2", name = "Team B")

        lateinit var listener: EventListener<QuerySnapshot>

        every {
            mockQuery.addSnapshotListener(any())
        } answers {
            listener = arg<EventListener<QuerySnapshot>>(0)
            mockRegistration
        }

        ActivityScenario.launch(MyTeamsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->

                // First snapshot
                val snap1 = mockk<QuerySnapshot>(relaxed = true)
                every { snap1.iterator() } returns mutableListOf(doc1).iterator()
                listener.onEvent(snap1, null)

                // Second snapshot
                val snap2 = mockk<QuerySnapshot>(relaxed = true)
                every { snap2.iterator() } returns mutableListOf(doc1, doc2).iterator()
                listener.onEvent(snap2, null)

                val rv = findViewByType(
                    activity.findViewById(android.R.id.content),
                    RecyclerView::class.java
                )

                assertEquals(2, rv!!.adapter!!.itemCount)
            }
        }
    }

    @Test
    fun teamAdapter_displaysCorrectMemberCount() {
        val team = Team(
            id = "t900",
            name = "Solo Team",
            members = listOf("one")
        )

        val parent = FrameLayout(RuntimeEnvironment.getApplication())
        val adapter = TeamAdapter(listOf(team)) {}

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        assertEquals("1 member", holder.tvTeamInfo.text.toString())
    }

    @Test
    fun teamAdapter_displaysPluralMembers() {
        val team = Team(
            id = "t901",
            name = "Group Team",
            members = listOf("one", "two")
        )

        val parent = FrameLayout(RuntimeEnvironment.getApplication())
        val adapter = TeamAdapter(listOf(team)) {}

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        assertEquals("2 members", holder.tvTeamInfo.text.toString())
    }

    @Test
    fun loadTeams_noUser_doesNothing() {
        every { mockAuth.currentUser } returns null

        ActivityScenario.launch(MyTeamsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // No crash expected
                assertTrue(true)
            }
        }
    }

    @Test
    fun loadTeams_skips_whenUserNull() {
        every { mockAuth.currentUser } returns null

        ActivityScenario.launch(MyTeamsActivity::class.java).use {
            assertTrue("No crash expected when uid is null", true)
        }
    }

    @Test
    fun adapter_click_invokesCallback() {
        var clickedTeam: Team? = null

        val team = Team(id = "xx1", name = "Alpha", members = listOf("a"))
        val adapter = TeamAdapter(listOf(team)) { clickedTeam = it }

        val parent = FrameLayout(RuntimeEnvironment.getApplication())
        val holder = adapter.onCreateViewHolder(parent, 0)

        adapter.onBindViewHolder(holder, 0)
        holder.itemView.performClick()

        assertEquals(team, clickedTeam)
    }

    @Test
    fun teamAdapter_bindsTeamNameCorrectly() {
        val team = Team(id = "1", name = "My Team", members = listOf("x"))
        val adapter = TeamAdapter(listOf(team)) {}

        val parent = FrameLayout(RuntimeEnvironment.getApplication())
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        assertEquals("My Team", holder.tvTeamName.text.toString())
    }

    private fun findTextViewWithText(root: ViewGroup, text: String): TextView? {
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)
            if (child is TextView && child.text == text) return child
            if (child is ViewGroup) {
                val result = findTextViewWithText(child, text)
                if (result != null) return result
            }
        }
        return null
    }

    private fun findButtonWithText(root: ViewGroup, text: String): View? {
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)
            if (child is Button && child.text == text) return child
            if (child is ViewGroup) {
                val result = findButtonWithText(child, text)
                if (result != null) return result
            }
        }
        return null
    }

    private fun <T> findViewByType(root: ViewGroup, clazz: Class<T>): T? {
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)

            if (clazz.isInstance(child)) {
                return clazz.cast(child)
            }

            if (child is ViewGroup) {
                val result = findViewByType(child, clazz)
                if (result != null) return result
            }
        }
        return null
    }
}
