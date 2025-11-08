package com.example.veato

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


/**
 * Firestore Integration Test
 * Features tested:
 * - Create team (leader auto-added)
 * - View members
 * - Leader/member can add/remove members
 * - Leader can start meal poll
 * - Query teams list
 * - Block unauthenticated or unauthorized actions
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class FirestoreIntegrationTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var userId: String

    @Before
    fun setup() = runTest {
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext())
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val email = "testuser@example.com"
        val password = "test1234"

        try {
            auth.signInWithEmailAndPassword(email, password).await()
            println("Logged in existing test user.")
        } catch (e: Exception) {
            println("Creating new test user...")
            auth.createUserWithEmailAndPassword(email, password).await()
        }

        userId = auth.currentUser?.uid ?: throw IllegalStateException("Auth failed: user is null.")
        println("Authenticated as UID: $userId")
    }

    // Create a new team — creator becomes leader & member
    @Test
    fun createTeam_success() = runTest {
        val teams = firestore.collection("teams")
        val teamId = "team_${System.currentTimeMillis()}"

        val teamData = mapOf(
            "teamName" to "Veato Test Team",
            "members" to listOf(userId),
            "leaderId" to userId,
            "pollActive" to false
        )

        teams.document(teamId).set(teamData).await()
        delay(1000)

        val snapshot = teams.document(teamId).get().await()
        Truth.assertThat(snapshot.exists()).isTrue()
        Truth.assertThat(snapshot.getString("leaderId")).isEqualTo(userId)
        Truth.assertThat((snapshot.get("members") as List<*>)).contains(userId)
        println("Team created successfully — leader & member verified.")
    }

    @Test
    fun membersCanViewTeam_success() = runTest {
        val teams = firestore.collection("teams")
        val teamId = "view_${System.currentTimeMillis()}"

        val teamData = mapOf(
            "teamName" to "Member List Test",
            "members" to listOf(userId, "memberX"),
            "leaderId" to userId,
            "pollActive" to false
        )

        teams.document(teamId).set(teamData).await()

        val snapshot = teams.document(teamId).get().await()
        val members = snapshot.get("members") as List<*>

        Truth.assertThat(snapshot.exists()).isTrue()
        Truth.assertThat(members).containsExactly(userId, "memberX")
        println("Members can view member list successfully.")
    }

    @Test
    fun leaderCanAddMember_success() = runTest {
        val teams = firestore.collection("teams")
        val teamId = "add_${System.currentTimeMillis()}"

        val teamData = mapOf(
            "teamName" to "Add Member Test",
            "members" to listOf(userId),
            "leaderId" to userId,
            "pollActive" to false
        )

        teams.document(teamId).set(teamData).await()

        // Leader adds a new member
        val newMember = "newMemberUid123"
        val snapshot = teams.document(teamId).get().await()
        val members =
            (snapshot.get("members") as? MutableList<String>)?.toMutableList() ?: mutableListOf()
        members.add(newMember)

        teams.document(teamId).update("members", members).await()
        delay(500)

        val updated = teams.document(teamId).get().await()
        val membersAfter = updated.get("members") as List<*>
        Truth.assertThat(membersAfter).contains(newMember)
        println("Leader successfully added a new member.")
    }

    @Test
    fun memberCanAddMember_success() = runTest {
        val teams = firestore.collection("teams")
        val teamId = "member_add_${System.currentTimeMillis()}"

        // 1. Create the leader user
        val leaderAuth = FirebaseAuth.getInstance()
        val leaderEmail = "leader_test@example.com"
        val leaderPassword = "leader1234"
        try {
            leaderAuth.signInWithEmailAndPassword(leaderEmail, leaderPassword).await()
        } catch (e: Exception) {
            leaderAuth.createUserWithEmailAndPassword(leaderEmail, leaderPassword).await()
        }
        val leaderId = leaderAuth.currentUser?.uid ?: throw Exception("Leader creation failed")

        // 2. Create a team as the leader
        val teamData = mapOf(
            "teamName" to "Member Add Test",
            "members" to listOf(leaderId),
            "leaderId" to leaderId,
            "pollActive" to false
        )
        firestore.collection("teams").document(teamId).set(teamData).await()
        delay(1000)

        // 3. Sign in as the member
        val memberAuth = FirebaseAuth.getInstance()
        val memberEmail = "member_test@example.com"
        val memberPassword = "member1234"
        try {
            memberAuth.signInWithEmailAndPassword(memberEmail, memberPassword).await()
        } catch (e: Exception) {
            memberAuth.createUserWithEmailAndPassword(memberEmail, memberPassword).await()
        }
        val memberId = memberAuth.currentUser?.uid ?: throw Exception("Member creation failed")

        // 4. As leader, add the member to the team
        firestore.collection("teams").document(teamId)
            .update("members", listOf(leaderId, memberId))
            .await()
        delay(1000)

        // 5. Reauthenticate as member, add a new member
        auth.signInWithEmailAndPassword(memberEmail, memberPassword).await()
        val snapshot = firestore.collection("teams").document(teamId).get().await()
        val members = (snapshot.get("members") as MutableList<String>).toMutableList()
        members.add("newMemberUid123") // simulate member adding another member
        firestore.collection("teams").document(teamId).update("members", members).await()

        val updated = firestore.collection("teams").document(teamId).get().await()
        val membersAfter = updated.get("members") as List<*>
        Truth.assertThat(membersAfter).contains("newMemberUid123")
        println("Member successfully added another member (no rule change).")
    }

    @Test
    fun memberCanRemoveMember_success() = runTest {
        val teams = firestore.collection("teams")
        val teamId = "member_remove_${System.currentTimeMillis()}"

        // 1. Create a leader user
        val leaderAuth = FirebaseAuth.getInstance()
        val leaderEmail = "leader_remove@example.com"
        val leaderPassword = "leader1234"
        try {
            leaderAuth.signInWithEmailAndPassword(leaderEmail, leaderPassword).await()
        } catch (e: Exception) {
            leaderAuth.createUserWithEmailAndPassword(leaderEmail, leaderPassword).await()
        }
        val leaderId = leaderAuth.currentUser?.uid ?: throw Exception("Leader creation failed")

        // 2. Create a team as leader with one member + one target
        val teamData = mapOf(
            "teamName" to "Removable Team",
            "members" to listOf(leaderId, userId, "targetMember123"),
            "leaderId" to leaderId,
            "pollActive" to false
        )
        firestore.collection("teams").document(teamId).set(teamData).await()
        delay(1000)

        // 3. Ensure current test user is in the team
        println("Current test user: $userId  |  Leader: $leaderId")

        // 4. Signed-in member removes another member
        val snapshot = firestore.collection("teams").document(teamId).get().await()
        val members = (snapshot.get("members") as MutableList<String>).toMutableList()
        members.remove("targetMember123")

        firestore.collection("teams").document(teamId).update("members", members).await()
        delay(500)

        val updated = firestore.collection("teams").document(teamId).get().await()
        val membersAfter = updated.get("members") as List<*>

        Truth.assertThat(updated.exists()).isTrue()
        Truth.assertThat(membersAfter).doesNotContain("targetMember123")
        Truth.assertThat(membersAfter).contains(userId)
        println("Member successfully removed another member.")
    }

    @Test
    fun leaderCanStartMealPoll_success() = runTest {
        val teams = firestore.collection("teams")
        val teamId = "poll_${System.currentTimeMillis()}"

        val teamData = mapOf(
            "teamName" to "Poll Test Team",
            "members" to listOf(userId),
            "leaderId" to userId,
            "pollActive" to false
        )

        teams.document(teamId).set(teamData).await()
        delay(500)

        // Leader starts poll
        teams.document(teamId).update("pollActive", true).await()

        val snapshot = teams.document(teamId).get().await()
        Truth.assertThat(snapshot.getBoolean("pollActive")).isTrue()
        println("Leader successfully started the meal poll.")
    }

    @Test
    fun unauthenticatedUser_cannotWriteTeam() = runTest {
        val tempAuth = FirebaseAuth.getInstance()
        tempAuth.signOut()
        val unauthFirestore = FirebaseFirestore.getInstance()

        val teamId = "unauth_${System.currentTimeMillis()}"
        val badTeam = mapOf(
            "teamName" to "Unauthorized Write",
            "members" to listOf("fakeUser"),
            "leaderId" to "fakeUser"
        )

        try {
            unauthFirestore.collection("teams").document(teamId).set(badTeam).await()
            assert(false) { "Expected failure for unauthenticated user." }
        } catch (e: Exception) {
            println("Unauthenticated user blocked as expected. Message: ${e.message}")
            Truth.assertThat(e.message?.contains("PERMISSION_DENIED")).isTrue()
            val msg = e.message ?: "No error message"
            println("Firestore unauthenticated write failed as expected. Message: $msg")
            Truth.assertThat(e).isInstanceOf(Exception::class.java)
            Truth.assertThat(msg).isNotEmpty()
        }
    }

    @Test
    fun listTeams_success() = runTest {
        val teams = firestore.collection("teams")

        for (i in 1..3) {
            val id = "list_${System.currentTimeMillis()}_$i"
            val data = mapOf(
                "teamName" to "Team $i",
                "members" to listOf(userId),
                "leaderId" to userId,
                "pollActive" to false
            )
            teams.document(id).set(data).await()
        }

        val result = teams.whereArrayContains("members", userId).get().await()
        Truth.assertThat(result.documents.size).isAtLeast(3)
        println("Retrieved ${result.documents.size} teams for user.")
    }
}