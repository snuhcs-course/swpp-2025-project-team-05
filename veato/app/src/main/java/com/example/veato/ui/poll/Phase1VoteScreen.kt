package com.example.veato.ui.poll

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun Phase1VoteScreen(
    state: PollScreenState,
    onToggleApproval: (String) -> Unit,
    onRejectCandidate: (String) -> Unit,
    onLockInVote: () -> Unit,
    onTimeOver: () -> Unit,
    onClearVetoAnimation: () -> Unit,
    onAcknowledgeReview: () -> Unit
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

    // Rejection confirmation dialog state
    var showRejectDialog by remember { mutableStateOf(false) }
    var candidateToReject by remember { mutableStateOf<String?>(null) }

    // Snackbar for rejection feedback
    val snackbarHostState = remember { SnackbarHostState() }

    // Track candidate list changes for animation
    val candidateNames = remember(poll.candidates) { poll.candidates.map { it.name } }
    var previousCandidates by remember { mutableStateOf(candidateNames) }

    // Detect when candidate list changes (menu was replaced)
    LaunchedEffect(candidateNames) {
        if (previousCandidates.isNotEmpty() && candidateNames != previousCandidates) {
            // Menu was changed - show snackbar
            snackbarHostState.showSnackbar(
                message = "Menu rejected! New menu loaded",
                duration = SnackbarDuration.Short
            )
        }
        previousCandidates = candidateNames
    }

    // Show error snackbar when veto fails
    LaunchedEffect(state.vetoError) {
        state.vetoError?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long
            )
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFFDC2626),
                    contentColor = Color.White
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
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
                Text(poll.title, style = MaterialTheme.typography.bodySmall)
                Text(
                    "Phase 1: Voting",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary),
                    fontWeight = FontWeight.SemiBold
                )
            }
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
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

        // Veto Banner
        if (state.rejectionUsed &&
            state.rejectedCandidateName != null &&
            state.newlyAddedCandidateName != null &&
            state.vetoAnimationTimestamp > 0) {
            VetoBanner(
                rejectedCandidateName = state.rejectedCandidateName,
                newCandidateName = state.newlyAddedCandidateName,
                onDismiss = onClearVetoAnimation
            )
        }

        // Needs Review Banner
        if (state.needsReview) {
            NeedsReviewBanner(
                invalidatedCandidates = state.invalidatedCandidateNames,
                onReview = onAcknowledgeReview
            )
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
                poll.candidates.forEach { candidate ->
                    Phase1CandidateRow(
                        name = candidate.name,
                        isApproved = candidate.name in state.selectedCandidateNames,
                        isRejected = state.rejectedCandidateName == candidate.name,
                        canReject = !state.rejectionUsed || state.rejectedCandidateName == candidate.name,
                        isLocked = state.voted || poll.hasCurrentUserLockedIn,
                        isVetoing = state.isVetoing,
                        isNewlyAdded = candidate.name == state.newlyAddedCandidateName,
                        onToggleApproval = { onToggleApproval(candidate.name) },
                        onReject = {
                            candidateToReject = candidate.name
                            showRejectDialog = true
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Lock In button
                Phase1LockInButton(
                    hasVoted = state.voted || poll.hasCurrentUserLockedIn,
                    needsReview = state.needsReview,
                    selectedCount = state.selectedCandidateNames.size,
                    onLockIn = onLockInVote
                )
            }
        }
        }
    }

    // Rejection confirmation dialog
    if (showRejectDialog && candidateToReject != null) {
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
                        candidateToReject?.let { onRejectCandidate(it) }
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
fun VetoBanner(
    rejectedCandidateName: String,
    newCandidateName: String?,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(true) }

    // Auto-dismiss after 5 seconds
    LaunchedEffect(Unit) {
        delay(5000)
        visible = false
        delay(300)  // Wait for exit animation
        onDismiss()
    }

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFEF2F2)  // Light red
            ),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(2.dp, Color(0xFFEF4444))  // Red border
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Menu Replaced!",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"$rejectedCandidateName\" removed",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF991B1B),
                        fontWeight = FontWeight.Medium
                    )
                    if (newCandidateName != null) {
                        Text(
                            text = "Replaced with \"$newCandidateName\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF15803D),  // Green
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                IconButton(onClick = { visible = false }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color(0xFF991B1B)
                    )
                }
            }
        }
    }
}

@Composable
fun NeedsReviewBanner(
    invalidatedCandidates: List<String>,
    onReview: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFEF2F2)  // Light red
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, Color(0xFFEF4444))  // Red border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "⚠️ Vote Invalidated",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFDC2626)
            )

            if (invalidatedCandidates.isNotEmpty()) {
                Text(
                    text = "The following menu(s) were vetoed and replaced:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF991B1B)
                )
                invalidatedCandidates.forEach { candidateName ->
                    Text(
                        text = "• \"$candidateName\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF991B1B),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            } else {
                Text(
                    text = "One or more menus you selected were vetoed and replaced.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF991B1B)
                )
            }

            Text(
                text = "Please review and update your selections.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF991B1B),
                fontWeight = FontWeight.Medium
            )

            Button(
                onClick = onReview,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0369A1)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Dismiss & Review")
            }
        }
    }
}

@Composable
fun Phase1CandidateRow(
    name: String,
    isApproved: Boolean,
    isRejected: Boolean,
    canReject: Boolean,
    isLocked: Boolean,
    isVetoing: Boolean,
    isNewlyAdded: Boolean = false,
    onToggleApproval: () -> Unit,
    onReject: () -> Unit
) {
    // Pulsing animation for newly added candidates
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Background color based on state
    val backgroundColor = when {
        isNewlyAdded -> Color(0xFFD1FAE5).copy(alpha = pulseAlpha)  // Pulsing green
        isRejected -> Color(0xFFFEE2E2)  // Light red
        else -> Color.Transparent
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(8.dp),
        border = if (isNewlyAdded) BorderStroke(2.dp, Color(0xFF10B981)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
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
                    onCheckedChange = { if (!isLocked && !isRejected) onToggleApproval() },
                    enabled = !isLocked && !isRejected,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))

                // Badge for new candidates
                if (isNewlyAdded) {
                    Text(
                        text = "NEW",
                        modifier = Modifier
                            .background(Color(0xFF10B981), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isApproved) FontWeight.SemiBold else FontWeight.Normal,
                        color = when {
                            isRejected -> Color.Gray
                            isNewlyAdded -> Color(0xFF065F46)  // Dark green
                            else -> Color.Black
                        }
                    )
                )
            }

            // Reject button (X icon or loading indicator)
            IconButton(
                onClick = onReject,
                enabled = !isLocked && canReject && !isRejected && !isVetoing,
                modifier = Modifier.size(40.dp)
            ) {
                if (isVetoing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                } else {
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
    }
}

@Composable
fun Phase1LockInButton(
    hasVoted: Boolean,
    needsReview: Boolean,
    selectedCount: Int,
    onLockIn: () -> Unit
) {
    if (!hasVoted) {
        val canLockIn = selectedCount > 0 && !needsReview
        val buttonColor = when {
            needsReview -> Color(0xFFDC2626)  // Red when needs review
            canLockIn -> MaterialTheme.colorScheme.primary
            else -> Color.LightGray
        }
        val buttonText = when {
            needsReview -> "Review selections before locking"
            else -> "$selectedCount selected  •  Lock In Vote"
        }

        Button(
            onClick = onLockIn,
            enabled = canLockIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonColor,
                disabledContainerColor = buttonColor.copy(alpha = 0.6f)
            )
        ) {
            Text(buttonText)
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
