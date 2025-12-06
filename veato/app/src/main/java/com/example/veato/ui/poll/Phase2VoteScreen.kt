package com.example.veato.ui.poll

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun Phase2VoteScreen(
    state: PollScreenState,
    onSelectCandidate: (Int) -> Unit,
    onLockInVote: () -> Unit,
    onTimeOver: () -> Unit
) {
    val poll = state.poll ?: return

    // Timer countdown
    var timeLeft by remember { mutableIntStateOf(poll.remainingTimeSeconds.toInt()) }
    LaunchedEffect(poll.remainingTimeSeconds) {
        timeLeft = poll.remainingTimeSeconds.toInt()
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

    val selectedIndex = state.selectedIndices.firstOrNull()

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
                    "Phase 2: Final Vote",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFEF4444)),
                    fontWeight = FontWeight.SemiBold
                )
            }
            Box(
                modifier = Modifier
                    .background(Color(0xFFEF4444), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(formattedTime, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // Locked-in counter
        if (poll.lockedInUserCount > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${poll.lockedInUserCount} member(s) have locked in their votes",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFDC2626)
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
                    text = "Final vote: Choose your top pick from these 3 finalists",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = "Vote counts from Phase 1 shown for reference",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Candidate list (Top 3)
                poll.candidates.take(3).forEachIndexed { index, candidate ->
                    Phase2CandidateRow(
                        name = candidate.name,
                        phase1Votes = candidate.phase1ApprovalCount,
                        index = index,
                        isSelected = selectedIndex == index,
                        isLocked = state.voted || poll.hasCurrentUserLockedIn,
                        onSelect = onSelectCandidate
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Lock In button
                Phase2LockInButton(
                    hasVoted = state.voted || poll.hasCurrentUserLockedIn,
                    hasSelected = selectedIndex != null,
                    onLockIn = onLockInVote
                )
            }
        }
    }
}

@Composable
fun Phase2CandidateRow(
    name: String,
    phase1Votes: Int,
    index: Int,
    isSelected: Boolean,
    isLocked: Boolean,
    onSelect: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFFEF2F2) else Color(0xFFF9FAFB)
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFEF4444))
        } else null,
        onClick = { if (!isLocked) onSelect(index) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = { if (!isLocked) onSelect(index) },
                    enabled = !isLocked,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Color(0xFFEF4444),
                        unselectedColor = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                    Text(
                        text = "$phase1Votes votes in Phase 1",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                    )
                }
            }
        }
    }
}

@Composable
fun Phase2LockInButton(
    hasVoted: Boolean,
    hasSelected: Boolean,
    onLockIn: () -> Unit
) {
    if (!hasVoted) {
        Button(
            onClick = onLockIn,
            enabled = hasSelected,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (hasSelected) Color(0xFFEF4444) else Color.LightGray
            )
        ) {
            Text(if (hasSelected) "Lock In Final Vote" else "Select a menu to lock in")
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "✓ Final vote locked in. Waiting for results...",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFDC2626)
            )
        }
    }
}
