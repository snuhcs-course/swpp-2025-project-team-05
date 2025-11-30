package com.example.veato

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.veato.ui.theme.VeatoTheme
import com.example.veato.ui.components.VeatoBottomNavigationBar
import com.example.veato.ui.components.NavigationScreen

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MyTeamsActivity : ComponentActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val teamsList = mutableListOf<Team>()
    private lateinit var adapter: TeamAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = TeamAdapter(
            teamsList,
            onTeamClick = { team -> navigateToTeamDetail(team) }
        )

        loadTeams()

        setContent {
            VeatoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MyTeamsScreen()
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MyTeamsScreen() {
        val context = LocalContext.current
        var showCreateDialog by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Teams",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        Button(
                            onClick = {
                                showCreateDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Create new team")
                        }
                    }
                )
            },
            bottomBar = {
                VeatoBottomNavigationBar(currentScreen = NavigationScreen.TEAMS)
            }
        ) { paddingValues ->
            // RecyclerView wrapped in AndroidView
            AndroidView(
                factory = { ctx ->
                    val density = ctx.resources.displayMetrics.density
                    val paddingPx = (16 * density).toInt()
                    RecyclerView(ctx).apply {
                        layoutManager = LinearLayoutManager(ctx)
                        adapter = this@MyTeamsActivity.adapter
                        setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                        clipToPadding = false
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }

        // Create Team Dialog
        if (showCreateDialog) {
            CreateTeamDialog(
                onDismiss = { showCreateDialog = false },
                onTeamCreated = { showCreateDialog = false }
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun CreateTeamDialog(
        onDismiss: () -> Unit,
        onTeamCreated: () -> Unit
    ) {
        val context = LocalContext.current
        var teamName by remember { mutableStateOf("") }
        var isCreating by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    "Create New Team",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Team Name Input
                    OutlinedTextField(
                        value = teamName,
                        onValueChange = { teamName = it },
                        label = { Text("Team Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = teamName.trim()
                        val uid = auth.currentUser?.uid

                        if (name.isEmpty()) {
                            Toast.makeText(context, "Please enter a team name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (uid == null) {
                            Toast.makeText(context, "You must be logged in", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isCreating = true
                        val teamRef = db.collection("teams").document()
                        val team = Team(
                            id = teamRef.id,
                            name = name,
                            leaderId = uid,
                            members = listOf(uid),
                            occasionType = "Other"
                        )

                        teamRef.set(team)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Team '$name' created!", Toast.LENGTH_SHORT).show()
                                isCreating = false
                                onTeamCreated()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                isCreating = false
                            }
                    },
                    enabled = !isCreating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Create")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isCreating
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    private fun loadTeams() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("teams")
            .whereArrayContains("members", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    //Toast.makeText(this, "Error loading teams", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                teamsList.clear()
                snapshot?.forEach { doc ->
                    val team = doc.toObject(Team::class.java).copy(id = doc.id)
                    teamsList.add(team)
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun navigateToTeamDetail(team: Team) {
        val intent = Intent(this, TeamDetailActivity::class.java)
        intent.putExtra("teamId", team.id)
        intent.putExtra("teamName", team.name)
        startActivity(intent)
    }
}
