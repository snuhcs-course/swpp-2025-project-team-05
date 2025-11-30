package com.example.veato.ui.poll

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay


@Composable
fun VoteScreen(
    state: PollScreenState,
    onSelect: (Int) -> Unit,
    onVote: () -> Unit,
    onCancel: () -> Unit,
    onTimeOver: () -> Unit
) {
    val poll = state.poll ?: return

    // Use backend remaining time when available; fall back to 60s if not provided
    var timeLeft by remember { mutableIntStateOf(state.poll?.duration ?: 60) }
    LaunchedEffect(state.poll?.duration) {
        timeLeft = state.poll?.duration ?: 60
    }
    LaunchedEffect(timeLeft) {
        if (timeLeft > 0 && (state.poll?.isOpen == true)) {
            delay(1000)
            timeLeft--
        } else if (timeLeft <= 0) {
            onTimeOver()
        }
    }
    val minutes = timeLeft / 60
    val seconds = timeLeft % 60
    val formattedTime = String.format("%d:%02d", minutes, seconds)



    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(poll.teamName, fontWeight = FontWeight.Bold)
                Text(poll.pollTitle, style = MaterialTheme.typography.bodySmall)
            }
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(formattedTime, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        }

        // candidate + vote button card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Vote from these 5 recommended menus.",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 4.dp)
                )


                poll.candidates.forEachIndexed { index, candidate ->
                    CandidateButton(
                        name = candidate.name,
                        index = index,
                        isSelected = index in state.selectedIndices,
                        isDisabled = state.voted && index !in state.selectedIndices,
                        onClick = onSelect
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                VoteButton(
                    voted = state.voted,
                    selectedCount = state.selectedIndices.size,
                    onVote = onVote,
                    onCancel = onCancel
                )
            }
        }
    }
}




@Composable
fun CandidateButton(
    name: String,
    index: Int,
    isSelected: Boolean,
    isDisabled: Boolean,
    onClick: (Int) -> Unit
) {
    OutlinedButton(
        onClick = { onClick(index) },
        enabled = !isDisabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = 1.dp,
            color = when {
                isDisabled -> MaterialTheme.colorScheme.outlineVariant
                isSelected -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outline
            }
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface

            },
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start // This aligns the content to the start (left)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = when {
                        isSelected -> FontWeight.SemiBold
                        isDisabled -> FontWeight.Light
                        else -> FontWeight.Normal
                    }
                )
            )
        }
    }
}

@Composable
fun VoteButton(
    voted: Boolean,
    selectedCount: Int,
    onVote: () -> Unit,
    onCancel: () -> Unit
) {
    if (!voted) {
        Button(
            onClick = onVote,
            enabled = selectedCount > 0,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text("$selectedCount selected  •  Vote")
        }
    } else {
        Button(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Cancel Vote")
        }
    }
}
