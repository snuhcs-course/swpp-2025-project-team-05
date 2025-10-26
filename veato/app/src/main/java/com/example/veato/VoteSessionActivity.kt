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
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.veato.data.repository.PollRepositoryDemo
import com.example.veato.ui.poll.PollResultScreen
import com.example.veato.ui.poll.PollViewModel
import com.example.veato.ui.poll.PollViewModelFactory
import com.example.veato.ui.poll.VoteScreen
import com.google.firebase.auth.FirebaseAuth

class VoteSessionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pollId = intent.getStringExtra("pollId") ?: "unknown_id"

        setContent {
            VeatoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PollSessionScreen(pollId = pollId)
                }
            }
        }
    }
}

@Preview
@Composable
fun PollSessionScreenPreview() {
    VeatoTheme {
        PollSessionScreen(pollId = "sample_poll_id")
    }
}


@Composable
fun PollSessionScreen(
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
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("loading data...", style = MaterialTheme.typography.titleMedium)
        }
    } else {
        if (state.poll?.isOpen == false) {
            PollResultScreen(state = state)
        } else if (state.poll?.isOpen == true) {
            VoteScreen(
                state = state,
                onSelect = { index ->
                    if (!state.voted) viewModel.modifySelectedIndices(index)
                },
                onVote = viewModel::sendBallot,
                onCancel = viewModel::revokeBallot
            )
        }
    }

}


