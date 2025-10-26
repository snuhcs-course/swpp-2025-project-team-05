package com.example.veato

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.veato.ui.theme.VeatoTheme
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.example.veato.data.local.ProfileDataStoreImpl
import com.example.veato.data.model.Candidate
import com.example.veato.data.remote.ProfileApiDataSource
import com.example.veato.data.repository.PollRepositoryDemo
import com.example.veato.data.repository.UserProfileRepositoryImpl
import com.example.veato.ui.poll.PollViewModel
import com.example.veato.ui.poll.PollViewModelFactory
import com.example.veato.ui.profile.ProfileViewModel
import com.example.veato.ui.profile.ProfileViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay


class VoteSessionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Intent에서 pollId 가져오기
        val pollId = intent.getStringExtra("pollId") ?: "unknown_id"

        setContent {
            VeatoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VoteSessionScreen(pollId = pollId)
                }
            }
        }
    }
}

@Preview
@Composable
fun VoteSessionScreenPreview() {
    VeatoTheme {
        VoteSessionScreen(pollId = "sample_poll_id")
    }
}




@Composable
fun VoteSessionScreen(
    pollId: String
) {
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "demo_user"

    val viewModel: PollViewModel = viewModel(
        factory = PollViewModelFactory(
            repository = PollRepositoryDemo(),
            userId = userId,
            pollId = pollId
        )
    )
    val state by viewModel.state.collectAsState()

    if (state.isBusy) {
        // 로딩 중 화면
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("loading data...", style = MaterialTheme.typography.titleMedium)
        }
    } else {
        // 실제 투표 UI

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 상단 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(state.poll?.teamName ?: "", fontWeight = FontWeight.Bold)
                    Text(state.poll?.pollTitle ?: "", style = MaterialTheme.typography.bodySmall)
                }
            }

            // 카드: 후보 + 투표 버튼
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
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
                    // 후보 버튼 리스트
                    state.poll?.candidates?.forEachIndexed { index, candidate ->
                        CandidateButton(
                            name = candidate.name,
                            index = index,
                            isSelected = index in state.selectedIndices,
                            isDisabled = state.voted && index !in state.selectedIndices,
                            onClick = {
                                if (!state.voted) {
                                    viewModel.modifySelectedIndices(index)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 투표 버튼
                    VoteButton(
                        voted = state.voted,
                        selectedCount = state.selectedIndices.size,
                        onVote = viewModel::sendBallot,
                        onCancel = viewModel::revokeBallot
                    )
                }
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
                isDisabled -> Color.LightGray
                isSelected -> Color(0xFF3DD1A0)
                else -> Color.Gray
            }
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = when {
                isSelected -> Color(0xFFF1FFF8)
                else -> Color(0xFFFDFDFD)

            },
            contentColor = Color.Black,
            disabledContentColor = Color.DarkGray
        )
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
                containerColor = if (selectedCount > 0) Color(0xFF3DD1A0) else Color.LightGray
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
            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
        ) {
            Text("Cancel Vote")
        }
    }
}


