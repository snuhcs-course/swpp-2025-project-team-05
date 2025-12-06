package com.example.veato

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.veato.ui.theme.VeatoTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.veato.data.di.DefaultRepositoryFactory
import com.example.veato.data.facade.VoteFlowFacade
import com.example.veato.ui.main.MainActivity
import com.example.veato.ui.poll.model.PollPhaseUi
import com.example.veato.ui.poll.Phase1VoteScreen
import com.example.veato.ui.poll.Phase2VoteScreen
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollSessionScreen(
    pollId: String
) {
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "demo_user"
    
    // Use Factory Method pattern to create repository and facade
    val repositoryFactory = remember { DefaultRepositoryFactory() }
    val facade = remember { 
        VoteFlowFacade(repositoryFactory.createPollRepository())
    }

    val viewModel: PollViewModel = viewModel(
        factory = PollViewModelFactory(
            facade = facade,
            userId = userId,
            pollId = pollId
        )
    )
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vote Session") },
                navigationIcon = {
                    IconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (state.isBusy) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("loading data...", style = MaterialTheme.typography.titleMedium)
        }
    } else {
        val poll = state.poll
        when (poll?.phase) {
            PollPhaseUi.PHASE1 -> {
                Phase1VoteScreen(
                    state = state,
                    onToggleApproval = { index ->
                        // Don't allow selecting a rejected candidate
                        val candidateName = poll.candidates.getOrNull(index)?.name
                        if (!state.voted && !poll.hasCurrentUserLockedIn && state.rejectedCandidateName != candidateName) {
                            viewModel.modifySelectedIndices(index)
                        }
                    },
                    onRejectCandidate = { index ->
                        viewModel.setRejectedCandidate(index)
                    },
                    onLockInVote = {
                        viewModel.submitPhase1Vote()
                    },
                    onTimeOver = { /* no-op: backend auto-closes; ViewModel observes status */ },
                    onClearVetoAnimation = {
                        viewModel.clearVetoAnimation()
                    }
                )
            }
            PollPhaseUi.PHASE2 -> {
                Phase2VoteScreen(
                    state = state,
                    onSelectCandidate = { index ->
                        if (!state.voted && !poll.hasCurrentUserLockedIn) {
                            // Clear previous selection and set new one (radio button behavior)
                            viewModel.clearSelectedIndices()
                            viewModel.modifySelectedIndices(index)
                        }
                    },
                    onLockInVote = {
                        viewModel.submitPhase2Vote()
                    },
                    onTimeOver = { /* no-op: backend auto-closes; ViewModel observes status */ }
                )
            }
            PollPhaseUi.CLOSED -> {
                PollResultScreen(
                    state = state,
                    onBackToMain = {
                        // Navigate back to TeamDetailActivity
                        val intent = Intent(context, TeamDetailActivity::class.java)
                        intent.putExtra("teamId", poll.teamId)
                        intent.putExtra("teamName", poll.teamName)
                        context.startActivity(intent)
                        (context as? VoteSessionActivity)?.finish()
                    }
                )
            }
            null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Error: Poll not found", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        }
        }
    }
}


