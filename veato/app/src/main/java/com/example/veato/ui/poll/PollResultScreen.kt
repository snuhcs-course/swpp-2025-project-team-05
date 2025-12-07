package com.example.veato.ui.poll

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PollResultScreen(
    state: PollScreenState,
    onBackToMain: () -> Unit
) {
    val poll = state.poll ?: return
    val winner = poll.results.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(poll.teamName, fontWeight = FontWeight.Bold)
                Text(poll.title, style = MaterialTheme.typography.bodySmall)
            }
            Box(
                modifier = Modifier
                    .background(Color(0xFFDC2626), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("Closed", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // Winner celebration card
        if (winner != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🏆 Winner 🏆",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )
                    )
                    Text(
                        text = winner.name,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF92400E)
                        )
                    )
                    if (winner.voteCount > 0) {
                        Text(
                            text = "${winner.voteCount} votes",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFFB45309)
                            )
                        )
                    }
                }
            }
        }

        // Complete results card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFDFD)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Complete Results",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Only display results for candidates that were actually votable
                val votableCandidateNames = poll.candidates.map { it.name }.toSet()
                val filteredResults = poll.results.filter { it.name in votableCandidateNames }

                filteredResults.forEachIndexed { index, candidate ->
                    ResultCandidateRow(
                        rank = index + 1,
                        name = candidate.name,
                        voteCount = candidate.voteCount,
                        isWinner = index == 0
                    )
                }
            }
        }


        Button(
            onClick = onBackToMain,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
        ) {
            Text("Back to Teams", color = Color.White)
        }
    }
}


@Composable
fun ResultCandidateRow(
    rank: Int,
    name: String,
    voteCount: Int,
    isWinner: Boolean
) {
    val bgColor = if (isWinner) Color(0xFFFFFBEB) else Color.White
    val borderColor = if (isWinner) Color(0xFFD97706) else Color.LightGray
    val textColor = if (isWinner) Color(0xFF92400E) else Color.Gray

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(8.dp))
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Rank badge
            Box(
                modifier = Modifier
                    .size(if (isWinner) 32.dp else 28.dp)
                    .background(
                        if (isWinner) Color(0xFFD97706) else Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(50)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isWinner) "🏆" else rank.toString(),
                    color = if (isWinner) Color.White else Color.Gray,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.width(12.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
                    color = textColor
                )
            )
        }

        // Vote count
        if (voteCount > 0) {
            Text(
                text = "$voteCount votes",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isWinner) FontWeight.SemiBold else FontWeight.Normal,
                    color = textColor
                )
            )
        }
    }
}

@Composable
fun CandidateBox(
    rank: Int,
    name: String,
    isWinner: Boolean
) {
    val bgColor = if (isWinner) Color(0xFFF1FFF8) else Color.White
    val borderColor = if (isWinner) MaterialTheme.colorScheme.primary else Color.LightGray
    val textColor = if (isWinner) Color(0xFF333333) else Color.Gray
    val scale = if (isWinner) 1.05f else 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(8.dp))
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(if (isWinner) 28.dp else 24.dp)
                .background(
                    if (isWinner) MaterialTheme.colorScheme.primary else Color(0xFFE0E0E0),
                    shape = RoundedCornerShape(50)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rank.toString(),
                color = if (isWinner) Color.White else Color.Gray,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
        )
    }
}
