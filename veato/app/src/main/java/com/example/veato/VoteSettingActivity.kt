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
                        onStartVoting = { title, duration ->
                            // TODO: 나중에 백엔드 API 호출해서 실제 pollId를 받아와야 함
                            println("Start voting: team=$teamName, title=$title, duration=$duration")
                            val pollId = "temp_poll_id" // 임시 pollId
                            val intent = Intent(this, VoteSessionActivity::class.java)
                            intent.putExtra("pollId", pollId)
                            startActivity(intent)
                            finish()
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
    onStartVoting: (String, Int) -> Unit
) {
    var sessionTitle by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("3") }
    val context = LocalContext.current

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
                    singleLine = true
                )

                // Duration
                Text(
                    text = "Poll Duration",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF374151))
                )

                SimpleDropdown(
                    options = listOf("1", "3", "5"),
                    selected = durationText,
                    onSelect = { durationText = it }
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
                    onStartVoting(sessionTitle, duration)
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF91CFC1)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Start New Poll")
        }
    }
}


@Composable
fun SimpleDropdown(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.width(160.dp)
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
