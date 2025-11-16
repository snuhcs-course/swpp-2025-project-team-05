package com.example.veato.ui.poll

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun Phase1VoteScreen(
    state: PollScreenState,
    onToggleApproval: (Int) -> Unit,
    onRejectCandidate: (Int) -> Unit,
    onLockInVote: () -> Unit,
    onTimeOver: () -> Unit
) {
    val poll = state.poll ?: return

    // Timer countdown
    var timeLeft by remember { mutableIntStateOf(poll.duration) }
    LaunchedEffect(poll.duration) {
        timeLeft = poll.duration
    }
    LaunchedEffect(timeLeft) {
        if (timeLeft > 0 && poll.isOpen) {
            delay(1000)
            timeLeft--
        } else if (timeLeft <= 0) {
            onTimeOver()
        }
    }
    val minutes = timeLeft / 60
    val seconds = timeLeft % 60
    val formattedTime = String.format("%d:%02d", minutes, seconds)

    // Rejection confirmation dialog state
    var showRejectDialog by remember { mutableStateOf(false) }
    var candidateToReject by remember { mutableIntStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(poll.teamName, fontWeight = FontWeight.Bold)
                Text(poll.pollTitle, style = MaterialTheme.typography.bodySmall)
                Text(
                    "Phase 1: Voting",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF3DD1A0)),
                    fontWeight = FontWeight.SemiBold
                )
            }
            Box(
                modifier = Modifier
                    .background(Color(0xFF3DD1A0), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(formattedTime, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // Locked-in counter
        if (poll.lockedInUserCount > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${poll.lockedInUserCount} member(s) have locked in their votes",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF0369A1)
                )
            }
        }

        // Main voting card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFDFD)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Vote for menus you like. You can also reject 1 menu.",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Candidate list
                poll.candidates.forEachIndexed { index, candidate ->
                    Phase1CandidateRow(
                        name = candidate.name,
                        index = index,
                        isApproved = index in state.selectedIndices,
                        isRejected = state.rejectedCandidateIndex == index,
                        canReject = !state.rejectionUsed || state.rejectedCandidateIndex == index,
                        isLocked = state.voted || poll.hasCurrentUserLockedIn,
                        onToggleApproval = onToggleApproval,
                        onReject = {
                            candidateToReject = index
                            showRejectDialog = true
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Lock In button
                Phase1LockInButton(
                    hasVoted = state.voted || poll.hasCurrentUserLockedIn,
                    selectedCount = state.selectedIndices.size,
                    onLockIn = onLockInVote
                )
            }
        }
    }

    // Rejection confirmation dialog
    if (showRejectDialog && candidateToReject >= 0) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = {
                Text("Reject Menu?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Are you sure you want to reject this menu? You can only reject one menu item for this poll.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRejectCandidate(candidateToReject)
                        showRejectDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Reject")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun Phase1CandidateRow(
    name: String,
    index: Int,
    isApproved: Boolean,
    isRejected: Boolean,
    canReject: Boolean,
    isLocked: Boolean,
    onToggleApproval: (Int) -> Unit,
    onReject: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Approval checkbox + name
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isApproved,
                onCheckedChange = { if (!isLocked && !isRejected) onToggleApproval(index) },
                enabled = !isLocked && !isRejected,
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF3DD1A0),
                    uncheckedColor = Color.Gray
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isApproved) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isRejected) Color.Gray else Color.Black
                )
            )
        }

        // Reject button (X icon)
        IconButton(
            onClick = onReject,
            enabled = !isLocked && canReject && !isRejected,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Reject",
                tint = when {
                    isRejected -> Color(0xFFDC2626)
                    !canReject -> Color.LightGray
                    else -> Color(0xFFEF4444)
                }
            )
        }
    }
}

@Composable
fun Phase1LockInButton(
    hasVoted: Boolean,
    selectedCount: Int,
    onLockIn: () -> Unit
) {
    if (!hasVoted) {
        Button(
            onClick = onLockIn,
            enabled = selectedCount > 0,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedCount > 0) Color(0xFF3DD1A0) else Color.LightGray
            )
        ) {
            Text("$selectedCount selected  •  Lock In Vote")
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2FE)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "✓ Vote locked in. Waiting for others...",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0369A1)
            )
        }
    }
}
