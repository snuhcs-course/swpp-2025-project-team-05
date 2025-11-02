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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.veato.data.repository.PollRepositoryImpl
import com.example.veato.ui.poll.PollViewModel
import com.example.veato.ui.poll.PollViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch


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


@Composable
fun VoteSettingScreen(
    teamName: String,
    teamId: String,
    onStartVoting: (String, Int) -> Unit
) {
    var sessionTitle by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("3") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Avoid recreating repository on every recomposition (causes typing lag)
    val repository = remember { PollRepositoryImpl() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Title
        Text(
            text = "New Poll Settings",
            style = MaterialTheme.typography.titleMedium.copy(color = Color.Gray)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Team Title
        Text(
            text = "$teamName - New Poll Settings",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4B5563)
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Card Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE9F3EF)),
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

                // Duration
                Text(
                    text = "Poll Duration",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF374151))
                )

                SimpleDropdown(
                    options = listOf("1", "3", "5"),
                    selected = durationText,
                    onSelect = { durationText = it },
                    enabled = !isLoading
                )

            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Start voting session Button
        Button(
            onClick = {
                val duration = durationText.toIntOrNull() ?: 3
                if (sessionTitle.isBlank()) {
                    Toast.makeText(context, "Poll Title cannot be empty", Toast.LENGTH_SHORT).show()
                } else {
                    isLoading = true
                    scope.launch {
                        try {
                            val response = repository.startVotingSession(teamId, sessionTitle, duration)
                            
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
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF91CFC1)
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


@Composable
fun SimpleDropdown(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { if (enabled) expanded = true },
            modifier = Modifier.width(160.dp),
            enabled = enabled
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$selected minutes")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (expanded)
                        Icons.Default.KeyboardArrowUp
                    else
                        Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text("$option minutes") },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
