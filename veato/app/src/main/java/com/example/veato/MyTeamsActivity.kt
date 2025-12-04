package com.example.veato

import android.content.Intent
import android.os.Bundle
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
import com.example.veato.ui.components.EXTRA_FROM_TAB_INDEX

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

class MyTeamsActivity : ComponentActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val teamsList = mutableListOf<Team>()
    private lateinit var adapter: TeamAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply transition animation based on tab direction
        applyTabTransition()

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
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

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
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
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
                snackbarHostState = snackbarHostState,
                scope = scope,
                onDismiss = { showCreateDialog = false },
                onTeamCreated = { showCreateDialog = false }
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun CreateTeamDialog(
        snackbarHostState: SnackbarHostState,
        scope: kotlinx.coroutines.CoroutineScope,
        onDismiss: () -> Unit,
        onTeamCreated: () -> Unit
    ) {
        val context = LocalContext.current
        var teamName by remember { mutableStateOf("") }
        var isCreating by remember { mutableStateOf(false) }
        var duplicateError by remember { mutableStateOf(false) }

        // Check for duplicate team names (case-insensitive, trimmed)
        LaunchedEffect(teamName) {
            val trimmedName = teamName.trim()
            if (trimmedName.isNotEmpty()) {
                duplicateError = teamsList.any {
                    it.name.trim().equals(trimmedName, ignoreCase = true)
                }
            } else {
                duplicateError = false
            }
        }

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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Team Name Input
                    OutlinedTextField(
                        value = teamName,
                        onValueChange = { teamName = it },
                        label = { Text("Team Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = duplicateError,
                        supportingText = if (duplicateError) {
                            { Text("A team with that name already exists.", color = MaterialTheme.colorScheme.error) }
                        } else null
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = teamName.trim()
                        val uid = auth.currentUser?.uid

                        if (name.isEmpty()) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Please enter a team name")
                            }
                            return@Button
                        }

                        if (uid == null) {
                            scope.launch {
                                snackbarHostState.showSnackbar("You must be logged in")
                            }
                            return@Button
                        }

                        // Double-check for duplicates (case-insensitive, trimmed)
                        if (teamsList.any { it.name.trim().equals(name, ignoreCase = true) }) {
                            scope.launch {
                                snackbarHostState.showSnackbar("A team with that name already exists.")
                            }
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
                                scope.launch {
                                    snackbarHostState.showSnackbar("Team '$name' created!")
                                }
                                isCreating = false
                                onTeamCreated()
                            }
                            .addOnFailureListener { e ->
                                scope.launch {
                                    snackbarHostState.showSnackbar("Failed: ${e.message}")
                                }
                                isCreating = false
                            }
                    },
                    enabled = !isCreating && !duplicateError,
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

    private fun applyTabTransition() {
        val fromIndex = intent.getIntExtra(EXTRA_FROM_TAB_INDEX, -1)
        val toIndex = NavigationScreen.TEAMS.index

        if (fromIndex != -1) {
            if (toIndex > fromIndex) {
                // Moving right: slide content from right to left
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            } else if (toIndex < fromIndex) {
                // Moving left: slide content from left to right
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            }
        }
    }
}
