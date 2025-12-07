package com.example.veato

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.veato.ui.theme.VeatoTheme
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.veato.data.di.DefaultRepositoryFactory
import com.example.veato.data.facade.VoteFlowFacade
import com.example.veato.ui.poll.PollViewModel
import com.example.veato.ui.poll.PollViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState


class VoteSettingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Intent에서 teamName, teamId 가져오기
        val teamName = intent.getStringExtra("teamName") ?: "Unknown Team"
        val teamId = intent.getStringExtra("teamId") ?: "unknown_id"

        setContent {
            VeatoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VoteSettingScreen(
                        teamName = teamName,
                        teamId = teamId,
                        onStartVoting = { title, duration ->
                            // This will be handled by the ViewModel
                        }
                    )
                }
            }
        }
    }
}


data class MemberInfo(
    val id: String,
    val name: String
)

@Composable
fun VoteSettingScreen(
    teamName: String,
    teamId: String,
    onStartVoting: (String, Int) -> Unit
) {
    var sessionTitle by remember { mutableStateOf("") }
    var occasionNote by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var teamMembers by remember { mutableStateOf<List<MemberInfo>>(emptyList()) }
    var selectedMembers by remember { mutableStateOf<Set<String>>(emptySet()) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { FirebaseFirestore.getInstance() }
    // Use Factory Method pattern to create repository and facade
    val repositoryFactory = remember { DefaultRepositoryFactory() }
    val facade = remember { 
        VoteFlowFacade(repositoryFactory.createPollRepository())
    }

    // Load team members with names
    LaunchedEffect(teamId) {
        try {
            val teamDoc = db.collection("teams").document(teamId).get().await()
            val memberIds = teamDoc.toObject(Team::class.java)?.members ?: emptyList()

            // Fetch member names from users collection
            val memberInfoList = memberIds.mapNotNull { memberId ->
                try {
                    val userDoc = db.collection("users").document(memberId).get().await()
                    val fullName = userDoc.getString("fullName") ?: userDoc.getString("username") ?: "Unknown"
                    MemberInfo(memberId, fullName)
                } catch (e: Exception) {
                    null
                }
            }

            teamMembers = memberInfoList
            selectedMembers = memberIds.toSet() // Select all members by default
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to load team members", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 48.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Start a New Poll",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4B5563)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Team subtitle
        Text(
            text = teamName,
            style = MaterialTheme.typography.titleMedium.copy(color = Color.Gray)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Card Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F2)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Session Title
                Text(
                    text = "Poll Title",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF374151))
                )
                OutlinedTextField(
                    value = sessionTitle,
                    onValueChange = { sessionTitle = it },
                    placeholder = { Text("e.g. 10/25 team dinner", color = Color.LightGray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading
                )

                // Poll Members Section
                Text(
                    text = "Poll Members",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF374151))
                )

                if (teamMembers.isEmpty()) {
                    Text(
                        text = "Loading members...",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        teamMembers.forEach { member ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedMembers.contains(member.id),
                                    onCheckedChange = { checked ->
                                        selectedMembers = if (checked) {
                                            selectedMembers + member.id
                                        } else {
                                            selectedMembers - member.id
                                        }
                                    },
                                    enabled = !isLoading
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = member.name,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Poll Details Section
                Text(
                    text = "Poll Details (optional)",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF374151))
                )
                OutlinedTextField(
                    value = occasionNote,
                    onValueChange = { occasionNote = it },
                    placeholder = { Text("e.g., high-protein and lower fat; less spicy for a baby; fast foods", color = Color.LightGray) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    enabled = !isLoading
                )

            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Start voting session Button
        Button(
            onClick = {
                // Validation
                when {
                    sessionTitle.isBlank() -> {
                        Toast.makeText(context, "Poll Title cannot be empty", Toast.LENGTH_SHORT).show()
                    }
                    selectedMembers.isEmpty() -> {
                        Toast.makeText(context, "At least 1 member must be selected", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        isLoading = true
                        scope.launch {
                            try {
                                val response = facade.startPoll(
                                    teamId = teamId,
                                    pollTitle = sessionTitle,
                                    durationMinutes = 3,  // Fixed duration (ignored by backend)
                                    includedMemberIds = selectedMembers.toList(),
                                    occasionNote = occasionNote.trim()
                                )

                                // Navigate to VoteSessionActivity with the pollId
                                val intent = Intent(context, VoteSessionActivity::class.java)
                                intent.putExtra("pollId", response.pollId)
                                context.startActivity(intent)
                                (context as? VoteSettingActivity)?.finish()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to start poll: ${e.message}", Toast.LENGTH_LONG).show()
                                isLoading = false
                            }
                        }
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFEB8765)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White
                )
            } else {
                Text("Start New Poll")
            }
        }
    }
}
