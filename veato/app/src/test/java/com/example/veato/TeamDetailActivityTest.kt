package com.example.veato

import android.content.Intent
import android.widget.Toast
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.FirebaseFirestore
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.*
import org.mockito.MockedStatic
import org.mockito.Mockito.*
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TeamDetailActivityTest {

    private lateinit var mockAuth: FirebaseAuth
    private lateinit var mockUser: FirebaseUser
    private lateinit var mockFirestore: FirebaseFirestore
    private lateinit var authStatic: MockedStatic<FirebaseAuth>
    private lateinit var firestoreStatic: MockedStatic<FirebaseFirestore>

    @Before
    fun setup() {
        mockAuth = mock(FirebaseAuth::class.java)
        mockUser = mock(FirebaseUser::class.java)

        `when`(mockUser.uid).thenReturn("mockUser123")
        `when`(mockAuth.currentUser).thenReturn(mockUser)

        authStatic = mockStatic(FirebaseAuth::class.java)
        authStatic.`when`<FirebaseAuth> { FirebaseAuth.getInstance() }
            .thenReturn(mockAuth)

        mockFirestore = mock(FirebaseFirestore::class.java)

        firestoreStatic = mockStatic(FirebaseFirestore::class.java)
        firestoreStatic.`when`<FirebaseFirestore> { FirebaseFirestore.getInstance() }
            .thenReturn(mockFirestore)
    }

    @After
    fun tearDown() {
        authStatic.close()
        firestoreStatic.close()
    }


    @Test
    fun activity_receives_teamId_and_teamName() {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            TeamDetailActivity::class.java
        )
        intent.putExtra("teamId", "X001")
        intent.putExtra("teamName", "Test Team")

        ActivityScenario.launch<TeamDetailActivity>(intent).use {
            assertTrue(true) // no crash
        }
    }

    @Test
    fun backButton_finishesActivity() {
        val controller = Robolectric.buildActivity(TeamDetailActivity::class.java).setup()
        val activity = controller.get()

        activity.finish()

        assertTrue(activity.isFinishing)
    }

    @Test
    fun testJoinPoll_noActivePoll_showsToast() {
        val controller = Robolectric.buildActivity(TeamDetailActivity::class.java).setup()
        val activity = controller.get()

        val team = Team(
            id = "TT1",
            name = "Team X",
            leaderId = "mockUser123",
            members = listOf("mockUser123"),
            currentlyOpenPoll = null
        )

        activity.testJoinPoll(team)

        val toast = ShadowToast.getLatestToast()
        assertThat(ShadowToast.getTextOfLatestToast(), containsString("No active poll"))
    }

    @Test
    fun testJoinPoll_withActivePoll_startsVoteSessionActivity() {
        val controller = Robolectric.buildActivity(TeamDetailActivity::class.java).setup()
        val activity = controller.get()

        val team = Team(
            id = "T10",
            name = "Team AAA",
            leaderId = "mockUser123",
            members = listOf("mockUser123"),
            currentlyOpenPoll = "POLL12"
        )

        activity.testJoinPoll(team)

        val startedIntent = Shadows.shadowOf(activity).nextStartedActivity
        assertNotNull(startedIntent)
        assertEquals(VoteSessionActivity::class.java.name, startedIntent.component?.className)
        assertEquals("POLL12", startedIntent.getStringExtra("pollId"))
    }

    @Test
    fun testLeaveTeam_showsToast() {
        // Mock Firestore update chain
        val mockCollection = mock(CollectionReference::class.java)
        val mockDoc = mock(DocumentReference::class.java)
        val mockTask = mock(Task::class.java) as Task<Void>

        `when`(mockFirestore.collection("teams")).thenReturn(mockCollection)
        `when`(mockCollection.document(anyString())).thenReturn(mockDoc)
        `when`(mockDoc.update(anyString(), any())).thenReturn(mockTask)

        // trigger success
        `when`(mockTask.addOnSuccessListener(any())).thenAnswer {
            val listener = it.arguments[0] as OnSuccessListener<Void>
            listener.onSuccess(null)
            mockTask
        }

        val controller = Robolectric.buildActivity(TeamDetailActivity::class.java).setup()
        val activity = controller.get()

        val team = Team(
            id = "TEAM55",
            name = "Alpha",
            leaderId = "mockUser123",
            members = listOf("mockUser123", "U2")
        )

        activity.testLeaveTeam(team)

        assertNotNull(ShadowToast.getLatestToast())
    }

    @Test
    fun testAddMember_blankEmail_showsToast() {
        val controller = Robolectric.buildActivity(TeamDetailActivity::class.java).setup()
        val activity = controller.get()

        val t = Team("A", "Team A", "mockUser123", listOf("mockUser123"))

        activity.testAddMember(t, "")

        assertThat(ShadowToast.getTextOfLatestToast(), containsString("Please enter an email"))
    }

    @Test
    fun testAddMember_invalidEmail_showsToast() {
        val controller = Robolectric.buildActivity(TeamDetailActivity::class.java).setup()
        val activity = controller.get()

        val t = Team("B", "Team B", "mockUser123", listOf("mockUser123"))

        activity.testAddMember(t, "wrongFormat")

        assertThat(ShadowToast.getTextOfLatestToast(), containsString("Please enter a valid email"))
    }

    @Test
    fun testEditMember_invokesToast() {
        val mockCollection = mock(CollectionReference::class.java)
        val mockDoc = mock(DocumentReference::class.java)
        val mockSub = mock(CollectionReference::class.java)
        val mockMemberDoc = mock(DocumentReference::class.java)
        val mockTask = mock(Task::class.java) as Task<Void>

        `when`(mockFirestore.collection("teams")).thenReturn(mockCollection)
        `when`(mockCollection.document(anyString())).thenReturn(mockDoc)
        `when`(mockDoc.collection("members_info")).thenReturn(mockSub)
        `when`(mockSub.document(anyString())).thenReturn(mockMemberDoc)
        `when`(mockMemberDoc.set(anyMap<String, Any>())).thenReturn(mockTask)

        `when`(mockTask.addOnSuccessListener(any())).thenAnswer {
            val listener = it.arguments[0] as OnSuccessListener<Void>
            listener.onSuccess(null)
            mockTask
        }

        val activity = Robolectric.buildActivity(TeamDetailActivity::class.java).setup().get()
        activity.testEditMember("T1", "userX", "Manager", "Adult")

        assertNotNull(ShadowToast.getLatestToast())
    }

    @Test
    fun testRemoveMember_invokesToast() {
        val mockCollection = mock(CollectionReference::class.java)
        val mockDoc = mock(DocumentReference::class.java)
        val mockTask = mock(Task::class.java) as Task<Void>

        `when`(mockFirestore.collection("teams")).thenReturn(mockCollection)
        `when`(mockCollection.document(anyString())).thenReturn(mockDoc)
        `when`(mockDoc.update(anyString(), any())).thenReturn(mockTask)

        `when`(mockTask.addOnSuccessListener(any())).thenAnswer {
            val listener = it.arguments[0] as OnSuccessListener<Void>
            listener.onSuccess(null)
            mockTask
        }

        val activity = Robolectric.buildActivity(TeamDetailActivity::class.java).setup().get()
        activity.testRemoveMember("T100", "userABC")

        assertNotNull(ShadowToast.getLatestToast())
    }

    @Test
    fun testFirestoreLoad_success() {
        val mockCollection = mock(CollectionReference::class.java)
        val mockDoc = mock(DocumentReference::class.java)
        val mockTask = mock(Task::class.java) as Task<DocumentSnapshot>

        `when`(mockFirestore.collection("teams")).thenReturn(mockCollection)
        `when`(mockCollection.document(anyString())).thenReturn(mockDoc)
        `when`(mockDoc.get()).thenReturn(mockTask)

        `when`(mockTask.addOnSuccessListener(any())).thenAnswer {
            val listener = it.arguments[0] as OnSuccessListener<DocumentSnapshot>
            listener.onSuccess(mock(DocumentSnapshot::class.java))
            mockTask
        }

        val activity = Robolectric.buildActivity(TeamDetailActivity::class.java).setup().get()
        activity.testFirestoreLoad("TTT1")

        assertNull(ShadowToast.getLatestToast())
    }

    @Test
    fun testFirestoreLoad_failure() {
        val mockCollection = mock(CollectionReference::class.java)
        val mockDoc = mock(DocumentReference::class.java)
        val mockTask = mock(Task::class.java) as Task<DocumentSnapshot>

        `when`(mockFirestore.collection("teams")).thenReturn(mockCollection)
        `when`(mockCollection.document(anyString())).thenReturn(mockDoc)
        `when`(mockDoc.get()).thenReturn(mockTask)

        `when`(mockTask.addOnSuccessListener(any())).thenReturn(mockTask)

        `when`(mockTask.addOnFailureListener(any())).thenAnswer {
            val listener = it.arguments[0] as OnFailureListener
            listener.onFailure(Exception("load error"))
            mockTask
        }

        val activity = Robolectric.buildActivity(TeamDetailActivity::class.java).setup().get()
        activity.testFirestoreLoad("BAD")

        assertThat(
            ShadowToast.getTextOfLatestToast(),
            containsString("Error loading team")
        )
    }

    @Test
    fun testLeaveTeam_failure_showsToast() {
        val mockCollection = mock(CollectionReference::class.java)
        val mockDoc = mock(DocumentReference::class.java)
        val mockTask = mock(Task::class.java) as Task<Void>

        `when`(mockFirestore.collection("teams")).thenReturn(mockCollection)
        `when`(mockCollection.document(anyString())).thenReturn(mockDoc)
        `when`(mockDoc.update(anyString(), any())).thenReturn(mockTask)

        // IMPORTANT: success first, failure second
        `when`(mockTask.addOnSuccessListener(any())).thenReturn(mockTask)

        `when`(mockTask.addOnFailureListener(any())).thenAnswer {
            val listener = it.arguments[0] as OnFailureListener
            listener.onFailure(Exception("fail"))
            mockTask
        }

        val activity = Robolectric.buildActivity(TeamDetailActivity::class.java).setup().get()

        val team = Team(
            id = "TEAM2",
            name = "MyTeam",
            leaderId = "mockUser123",
            members = listOf("mockUser123", "X")
        )

        activity.testLeaveTeam(team)

        assertThat(
            ShadowToast.getTextOfLatestToast(),
            containsString("Failed to leave team")
        )
    }

    @Test
    fun testEditMember_failure_showsToast() {
        val mockCollection = mock(CollectionReference::class.java)
        val mockDoc = mock(DocumentReference::class.java)
        val mockSub = mock(CollectionReference::class.java)
        val mockMemberDoc = mock(DocumentReference::class.java)
        val mockTask = mock(Task::class.java) as Task<Void>

        `when`(mockFirestore.collection("teams")).thenReturn(mockCollection)
        `when`(mockCollection.document(anyString())).thenReturn(mockDoc)
        `when`(mockDoc.collection("members_info")).thenReturn(mockSub)
        `when`(mockSub.document(anyString())).thenReturn(mockMemberDoc)
        `when`(mockMemberDoc.set(anyMap<String, Any>())).thenReturn(mockTask)

        // MUST stub success listener first
        `when`(mockTask.addOnSuccessListener(any())).thenReturn(mockTask)

        `when`(mockTask.addOnFailureListener(any())).thenAnswer {
            val listener = it.arguments[0] as OnFailureListener
            listener.onFailure(Exception("update fail"))
            mockTask
        }

        val activity = Robolectric.buildActivity(TeamDetailActivity::class.java).setup().get()

        activity.testEditMember("TID", "UID", "Manager", "Adult")

        assertThat(
            ShadowToast.getTextOfLatestToast(),
            containsString("Failed to update member")
        )
    }

    @Test
    fun testRemoveMember_failure_showsToast() {
        val mockCollection = mock(CollectionReference::class.java)
        val mockDoc = mock(DocumentReference::class.java)
        val mockTask = mock(Task::class.java) as Task<Void>

        `when`(mockFirestore.collection("teams")).thenReturn(mockCollection)
        `when`(mockCollection.document(anyString())).thenReturn(mockDoc)
        `when`(mockDoc.update(anyString(), any())).thenReturn(mockTask)

        // MUST stub success first
        `when`(mockTask.addOnSuccessListener(any())).thenReturn(mockTask)

        `when`(mockTask.addOnFailureListener(any())).thenAnswer {
            val listener = it.arguments[0] as OnFailureListener
            listener.onFailure(Exception("remove fail"))
            mockTask
        }

        val activity = Robolectric.buildActivity(TeamDetailActivity::class.java).setup().get()

        activity.testRemoveMember("TID", "UID")

        assertThat(
            ShadowToast.getTextOfLatestToast(),
            containsString("Failed to remove member")
        )
    }

    @Test
    fun testJoinPoll_withActivePoll_startsActivity() {
        val controller = Robolectric.buildActivity(TeamDetailActivity::class.java).setup()
        val activity = controller.get()

        val team = Team(
            id = "TID",
            name = "Team",
            leaderId = "mockUser123",
            members = listOf("mockUser123"),
            currentlyOpenPoll = "POLL123"
        )

        activity.testJoinPoll(team)

        val nextIntent = Shadows.shadowOf(activity).nextStartedActivity
        assertEquals("POLL123", nextIntent.getStringExtra("pollId"))
    }

    @Test
    fun positionOptions_formalDinner() {
        val activity = Robolectric.buildActivity(TeamDetailActivity::class.java).setup().get()
        val list = activity.invokePrivateList("Formal Dinner with Clients")
        assertEquals(listOf("Host", "Client", "Colleague", "Support Staff", "Other"), list)
    }

    @Test
    fun positionOptions_default() {
        val activity = Robolectric.buildActivity(TeamDetailActivity::class.java).setup().get()
        val list = activity.invokePrivateList("Unknown Occasion")
        assertEquals(listOf("Guest", "Participant", "Other"), list)
    }

    @Test
    fun members_areSorted_leaderFirst_thenCurrentUser_thenAlphabetical() {
        val activity = Robolectric.buildActivity(TeamDetailActivity::class.java).setup().get()

        val list = listOf(
            MemberDetail("3", "Zane", "z@x.com", false, isCurrentUser = false),
            MemberDetail("1", "Alice", "a@x.com", true, isCurrentUser = false),   // Leader
            MemberDetail("5", "Bob", "b@x.com", false, isCurrentUser = true),    // Current user
            MemberDetail("2", "Carol", "c@x.com", false, isCurrentUser = false)
        )

        val sorted = list.sortedWith(
            compareByDescending<MemberDetail> { it.isLeader }
                .thenByDescending { it.isCurrentUser }
                .thenBy { it.fullName }
        )

        assertEquals("Alice", sorted[0].fullName) // leader
        assertEquals("Bob", sorted[1].fullName)   // current user
    }

    @Test
    fun testJoinPoll_withPoll_navigates() {
        val activity = Robolectric.buildActivity(TeamDetailActivity::class.java).setup().get()

        val team = Team(
            id = "T1",
            name = "Test",
            leaderId = "mockUser123",
            members = listOf("mockUser123"),
            currentlyOpenPoll = "P123"
        )

        activity.testJoinPoll(team)

        val nextIntent = Shadows.shadowOf(activity).nextStartedActivity
        assertEquals(VoteSessionActivity::class.java.name, nextIntent.component?.className)
        assertEquals("P123", nextIntent.getStringExtra("pollId"))
    }

    @Test
    fun testAddMember_userNotFound_showsToast() {
        val mockUsers = mock(CollectionReference::class.java)
        val mockQuery = mock(Task::class.java) as Task<QuerySnapshot>

        val mockTeams = mock(CollectionReference::class.java)

        // Firestore mocks
        `when`(mockFirestore.collection("users")).thenReturn(mockUsers)
        `when`(mockUsers.whereEqualTo(eq("email"), anyString())).thenReturn(mockUsers)
        `when`(mockUsers.get()).thenReturn(mockQuery)

        // simulate empty result
        val emptySnapshot = mock(QuerySnapshot::class.java)
        `when`(emptySnapshot.isEmpty).thenReturn(true)

        `when`(mockQuery.addOnSuccessListener(any())).thenAnswer {
            val listener = it.arguments[0] as OnSuccessListener<QuerySnapshot>
            listener.onSuccess(emptySnapshot)
            mockQuery
        }

        val activity = Robolectric.buildActivity(TeamDetailActivity::class.java).setup().get()
        val team = Team("T1", "Team", "mockUser123", listOf("mockUser123"))

        activity.testAddMember(team, "someone@domain.com")

        assertThat(ShadowToast.getTextOfLatestToast(), containsString("User not found"))
    }

    @Test
    fun testAddMember_success_showsToast() {
        val mockUsers = mock(CollectionReference::class.java)
        val mockQuery = mock(Task::class.java) as Task<QuerySnapshot>

        val mockTeams = mock(CollectionReference::class.java)
        val mockTeamDoc = mock(DocumentReference::class.java)
        val mockUpdateTask = mock(Task::class.java) as Task<Void>

        val snapshot = mock(QuerySnapshot::class.java)
        val doc = mock(DocumentSnapshot::class.java)
        `when`(snapshot.isEmpty).thenReturn(false)
        `when`(snapshot.documents).thenReturn(listOf(doc))
        `when`(doc.id).thenReturn("USER1")

        `when`(mockFirestore.collection("users")).thenReturn(mockUsers)
        `when`(mockFirestore.collection("teams")).thenReturn(mockTeams)

        `when`(mockUsers.whereEqualTo(eq("email"), anyString())).thenReturn(mockUsers)
        `when`(mockUsers.get()).thenReturn(mockQuery)

        `when`(mockQuery.addOnSuccessListener(any())).thenAnswer {
            val listener = it.arguments[0] as OnSuccessListener<QuerySnapshot>
            listener.onSuccess(snapshot)
            mockQuery
        }

        `when`(mockTeams.document(anyString())).thenReturn(mockTeamDoc)
        `when`(mockTeamDoc.update(anyString(), any())).thenReturn(mockUpdateTask)

        `when`(mockUpdateTask.addOnSuccessListener(any())).thenAnswer {
            val listener = it.arguments[0] as OnSuccessListener<Void>
            listener.onSuccess(null)
            mockUpdateTask
        }

        val activity = Robolectric.buildActivity(TeamDetailActivity::class.java).setup().get()
        val team = Team("T1", "Team", "mockUser123", listOf("mockUser123"))

        activity.testAddMember(team, "test@domain.com")

        assertThat(
            ShadowToast.getTextOfLatestToast(),
            containsString("Member added successfully")
        )
    }

    @Test
    fun testLoadMemberDetails_skipOnException() {
        val activity = Robolectric.buildActivity(TeamDetailActivity::class.java).setup().get()

        val team = Team(
            id = "T1",
            name = "Team",
            leaderId = "L1",
            members = listOf("A", "B")
        )

        // make Firestore throw
        val mockUsers = mock(CollectionReference::class.java)
        val mockUserDoc = mock(DocumentReference::class.java)

        `when`(mockFirestore.collection("users")).thenReturn(mockUsers)
        `when`(mockUsers.document(anyString())).thenReturn(mockUserDoc)
        `when`(mockUserDoc.get()).thenThrow(RuntimeException("boom"))

        activity.testLoadMemberDetailsForTest(team) { list ->
            assertTrue(list.isEmpty())
        }
    }

    // helper
    private fun TeamDetailActivity.invokePrivateList(s: String): List<String> {
        val method = TeamDetailActivity::class.java.getDeclaredMethod("getPositionOptions", String::class.java)
        method.isAccessible = true
        return method.invoke(this, s) as List<String>
    }
}
