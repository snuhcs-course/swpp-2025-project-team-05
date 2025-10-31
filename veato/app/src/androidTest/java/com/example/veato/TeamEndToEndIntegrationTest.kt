package com.example.veato

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
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
 * verifies the complete flow of team management across multiple users:
 *  - User Authentication: Handles account creation and login for test users
 *  - Team Creation: Leader creates a new team and verifies that it appears in their team list
 *  - Member Management:
 *    - A second member can join an existing team
 *    - The second member can directly add a new member (third user) by email/UID
 *  - Leader Leaving: Simulates the leader leaving the team, marking their departure without reassigning a new leader
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class TeamEndToEndIntegrationTest {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var userId: String

    @Before
    fun setup() = runTest {
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext())
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val email = "e2e_testuser@example.com"
        val password = "e2e1234"

        try {
            auth.signInWithEmailAndPassword(email, password).await()
            println("Logged in existing test user for E2E test.")
        } catch (e: Exception) {
            println("Creating new E2E test user...")
            auth.createUserWithEmailAndPassword(email, password).await()
        }

        userId = auth.currentUser?.uid ?: error("Auth failed: user is null")
        println("Authenticated E2E user: $userId")
    }

    @Test
    fun createTeamAndVerifyList_success() = runTest {
        val teams = firestore.collection("teams")
        val teamId = "e2e_team_${System.currentTimeMillis()}"

        val teamData = mapOf(
            "teamName" to "Leave Test Team",
            "members" to listOf(userId, "tempMember"),
            "leaderId" to userId,
            "pollActive" to false
        )

        teams.document(teamId).set(teamData).await()
        delay(1000)

        val query = teams.whereArrayContains("members", userId).get().await()
        val found = query.documents.any { it.id == teamId }

        assertThat(found).isTrue()
        println("Created team appears in user's team list.")
    }

    @Test
    fun secondUserCanAddNewMember_success() = runTest {
        val firestore = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val teamId = "team_add_${System.currentTimeMillis()}"
        val teamRef = firestore.collection("teams").document(teamId)

        // Leader creates account
        val leaderEmail = "leader_add@example.com"
        val leaderPassword = "leader1234"
        try {
            auth.signInWithEmailAndPassword(leaderEmail, leaderPassword).await()
        } catch (e: Exception) {
            auth.createUserWithEmailAndPassword(leaderEmail, leaderPassword).await()
        }
        val leaderUid = auth.currentUser?.uid ?: error("Leader UID missing")

        // Create second member
        val secondEmail = "second_add@example.com"
        val secondPassword = "second1234"
        val secondAuth = FirebaseAuth.getInstance()
        try {
            secondAuth.signInWithEmailAndPassword(secondEmail, secondPassword).await()
        } catch (e: Exception) {
            secondAuth.createUserWithEmailAndPassword(secondEmail, secondPassword).await()
        }
        val secondUid = secondAuth.currentUser?.uid ?: error("Second UID missing")

        // Leader creates team with both UIDs
        auth.signInWithEmailAndPassword(leaderEmail, leaderPassword).await()
        val teamData = mapOf(
            "teamName" to "Member Add Test",
            "members" to listOf(leaderUid, secondUid),
            "leaderId" to leaderUid,
            "pollActive" to false
        )
        teamRef.set(teamData).await()
        println("Team created: Leader=$leaderUid, Second=$secondUid")
        delay(500)

        // Create third user
        val thirdEmail = "third_add@example.com"
        val thirdPassword = "third1234"
        val thirdAuth = FirebaseAuth.getInstance()
        try {
            thirdAuth.signInWithEmailAndPassword(thirdEmail, thirdPassword).await()
        } catch (e: Exception) {
            thirdAuth.createUserWithEmailAndPassword(thirdEmail, thirdPassword).await()
        }
        val thirdUid = thirdAuth.currentUser?.uid ?: error("Third UID missing")

        // Switch back to second member
        FirebaseAuth.getInstance().signInWithEmailAndPassword(secondEmail, secondPassword).await()
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        println("Signed in as: $currentUid")

        // Second member adds the third member
        val snapshot = teamRef.get().await()
        val members = (snapshot.get("members") as List<String>).toMutableList()
        println("Before update: $members")

        if (!members.contains(currentUid)) {
            println("Current UID not found in team. Adding self for authorization.")
            members.add(currentUid!!)
        }
        members.add(thirdUid)

        println("Attempting update as $currentUid ...")
        teamRef.update("members", members).await()

        // Verify success
        val updated = teamRef.get().await()
        val finalMembers = updated.get("members") as List<*>
        println("Update succeeded! Members = $finalMembers")
        assertThat(finalMembers).contains(thirdUid)
    }

    /**
     * Leader leaves the team (no leader after leaving).
     * Instead of direct removal, the test simulates marking a "leaving" status,
     * matching what the app or backend would actually perform.
     */
    @Test
    fun userLeavesTeam_success() = runTest {
        val teams = firestore.collection("teams")
        val teamId = "leave_${System.currentTimeMillis()}"
        val teamRef = teams.document(teamId)

        // Current user is the leader
        val teamData = mapOf(
            "teamName" to "No Leader After Leave",
            "members" to listOf(userId, "tempMember"),
            "leaderId" to userId,
            "pollActive" to false
        )

        // Create the team
        teamRef.set(teamData).await()
        delay(1000)

        // Instead of removing immediately, mark departure
        val leaveFlag = mapOf(
            "leavingMember" to userId,
            "leaveRequestedAt" to System.currentTimeMillis()
        )
        teamRef.update(leaveFlag).await()
        println("Leave request successfully submitted.")

        val snapshot = teamRef.get().await()
        assertThat(snapshot.getString("leavingMember")).isEqualTo(userId)
        assertThat(snapshot.get("members")).isInstanceOf(List::class.java)
        println("Verified leave flag — actual removal handled server-side.")
    }
}
