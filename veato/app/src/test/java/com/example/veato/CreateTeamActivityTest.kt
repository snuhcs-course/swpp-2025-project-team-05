package com.example.veato

import android.widget.*
import androidx.test.core.app.ActivityScenario
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
class CreateTeamActivityTest {

    private lateinit var mockAuth: FirebaseAuth
    private lateinit var mockDb: FirebaseFirestore
    private lateinit var mockTeamRef: DocumentReference

    @Before
    fun setup() {
        mockkStatic(FirebaseAuth::class)
        mockkStatic(FirebaseFirestore::class)

        mockAuth = mockk(relaxed = true)
        mockDb = mockk(relaxed = true)
        mockTeamRef = mockk(relaxed = true)

        every { FirebaseAuth.getInstance() } returns mockAuth
        every { FirebaseFirestore.getInstance() } returns mockDb
        every { mockDb.collection("teams").document() } returns mockTeamRef
    }

    @After
    fun teardown() = unmockkAll()


    @Test
    fun showError_WhenTeamNameIsEmpty() {
        val scenario = ActivityScenario.launch(CreateTeamActivity::class.java)

        scenario.onActivity { activity ->
            val nameInput = activity.findViewById<EditText>(R.id.editTeamName)
            val btnCreate = activity.findViewById<Button>(R.id.btnCreateTeam)

            nameInput.setText("")
            btnCreate.performClick()

            assertEquals("Please enter a team name", ShadowToast.getTextOfLatestToast())
        }
    }

    @Test
    fun showError_WhenNoOccasionSelected() {
        val scenario = ActivityScenario.launch(CreateTeamActivity::class.java)

        scenario.onActivity { activity ->
            val nameInput = activity.findViewById<EditText>(R.id.editTeamName)
            val spinner = activity.findViewById<Spinner>(R.id.spinnerOccasionType)
            val btnCreate = activity.findViewById<Button>(R.id.btnCreateTeam)

            nameInput.setText("My Team")

            // simulate spinner selecting "Select One..."
            spinner.setSelection(0)

            btnCreate.performClick()

            assertEquals("Please select an occasion type", ShadowToast.getTextOfLatestToast())
        }
    }

    @Test
    fun showError_WhenUserNotLoggedIn() {
        every { mockAuth.currentUser } returns null

        val scenario = ActivityScenario.launch(CreateTeamActivity::class.java)

        scenario.onActivity { activity ->
            val nameInput = activity.findViewById<EditText>(R.id.editTeamName)
            val spinner = activity.findViewById<Spinner>(R.id.spinnerOccasionType)
            val btnCreate = activity.findViewById<Button>(R.id.btnCreateTeam)

            nameInput.setText("My Team")
            spinner.setSelection(1)

            btnCreate.performClick()

            assertEquals("You must be logged in to create a team", ShadowToast.getTextOfLatestToast())
        }
    }

    @Test
    fun createTeam_Success() {
        val fakeUser = mockk<com.google.firebase.auth.FirebaseUser>()
        every { fakeUser.uid } returns "uid123"
        every { mockAuth.currentUser } returns fakeUser

        // Create a fake Task<Void> that immediately invokes onSuccess
        val fakeTask = mockk<com.google.android.gms.tasks.Task<Void>>(relaxed = true)

        every { mockTeamRef.set(any()) } returns fakeTask

        every { fakeTask.addOnSuccessListener(any()) } answers {
            val listener = arg<OnSuccessListener<Void>>(0)
            listener.onSuccess(null)
            fakeTask
        }

        val scenario = ActivityScenario.launch(CreateTeamActivity::class.java)

        scenario.onActivity { activity ->
            val nameInput = activity.findViewById<EditText>(R.id.editTeamName)
            val spinner = activity.findViewById<Spinner>(R.id.spinnerOccasionType)
            val btnCreate = activity.findViewById<Button>(R.id.btnCreateTeam)

            nameInput.setText("Veato Team")
            spinner.setSelection(1)

            btnCreate.performClick()

            // Let Robolectric flush async tasks
            org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            assertEquals(
                "Team 'Veato Team' created successfully!",
                ShadowToast.getTextOfLatestToast()
            )

            verify(exactly = 1) { mockTeamRef.set(any()) }
        }
    }

    @Test
    fun spinner_DefaultValue_IsSelectOne() {
        val scenario = ActivityScenario.launch(CreateTeamActivity::class.java)

        scenario.onActivity { activity ->
            val spinner = activity.findViewById<Spinner>(R.id.spinnerOccasionType)
            val firstItem = spinner.adapter.getItem(0) as String

            assertEquals("Select One...", firstItem)
        }
    }
}
