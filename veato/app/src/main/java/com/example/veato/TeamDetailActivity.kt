package com.example.veato

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.veato.ui.theme.VeatoTheme
import com.example.veato.ui.components.VeatoBottomNavigationBar
import com.example.veato.ui.components.NavigationScreen

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class MemberDetail(
    val userId: String,
    val fullName: String,
    val userName: String,
    val email: String,
    val isLeader: Boolean,
    val position: String? = null,
    val ageGroup: String? = null,
    val isCurrentUser: Boolean = false
)

class TeamDetailActivity : ComponentActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val teamId = intent.getStringExtra("teamId") ?: ""
        val teamName = intent.getStringExtra("teamName") ?: "Team"

        setContent {
            VeatoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TeamDetailScreen(
                        teamId = teamId,
                        teamName = teamName
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TeamDetailScreen(teamId: String, teamName: String) {
        val context = LocalContext.current
        var team by remember { mutableStateOf<Team?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var showLeaveDialog by remember { mutableStateOf(false) }
        val currentUserId = auth.currentUser?.uid
        val isLeader = team?.leaderId == currentUserId

        // Load team data
        LaunchedEffect(teamId) {
            db.collection("teams").document(teamId)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        team = doc.toObject(Team::class.java)?.copy(id = doc.id)
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error loading team", Toast.LENGTH_SHORT).show()
                    isLoading = false
                }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            teamName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        // Show Leave Team button only for non-leaders
                        if (team != null && !isLeader) {
                            IconButton(onClick = { showLeaveDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = "Leave Team"
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                VeatoBottomNavigationBar(currentScreen = NavigationScreen.TEAMS)
            }
        ) { paddingValues ->
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                team == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Team not found")
                    }
                }
                else -> {
                    TeamDetailContent(
                        team = team!!,
                        paddingValues = paddingValues,
                        onStartPoll = {
                            val intent = Intent(context, VoteSettingActivity::class.java)
                            intent.putExtra("teamId", team!!.id)
                            intent.putExtra("teamName", team!!.name)
                            context.startActivity(intent)
                        },
                        onJoinPoll = {
                            val pollId = team!!.currentlyOpenPoll
                            if (pollId != null) {
                                val intent = Intent(context, VoteSessionActivity::class.java)
                                intent.putExtra("pollId", pollId)
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, "No active poll", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }

        // Leave team confirmation dialog
        if (showLeaveDialog) {
            AlertDialog(
                onDismissRequest = { showLeaveDialog = false },
                title = { Text("Leave this team?") },
                text = { Text("Are you sure you want to leave this team permanently? You'll be removed from the member list and will no longer see this team.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLeaveDialog = false
                            leaveTeam(team!!)
                        }
                    ) {
                        Text("Leave team", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLeaveDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    @Composable
    private fun TeamDetailContent(
        team: Team,
        paddingValues: PaddingValues,
        onStartPoll: () -> Unit,
        onJoinPoll: () -> Unit
    ) {
        val context = LocalContext.current
        val currentUserId = auth.currentUser?.uid
        val isLeader = team.leaderId == currentUserId

        // State for member management
        var memberDetails by remember { mutableStateOf<List<MemberDetail>>(emptyList()) }
        var isLoadingMembers by remember { mutableStateOf(true) }
        var emailToAdd by remember { mutableStateOf("") }
        var showEditMemberDialog by remember { mutableStateOf<MemberDetail?>(null) }
        var showRemoveMemberDialog by remember { mutableStateOf<MemberDetail?>(null) }
        var refreshTrigger by remember { mutableStateOf(0) }

        // Load members with their details
        LaunchedEffect(team.id, refreshTrigger) {
            isLoadingMembers = true
            try {
                // Reload team data to get latest member list
                val latestTeamDoc = db.collection("teams").document(team.id).get().await()
                val latestMembers = latestTeamDoc.get("members") as? List<*> ?: emptyList<Any>()

                val memberInfoList = mutableListOf<MemberDetail>()

                for (memberId in latestMembers.filterIsInstance<String>()) {
                    try {
                        // Fetch user document
                        val userDoc = db.collection("users").document(memberId).get().await()
                        val fullName = userDoc.getString("fullName") ?: userDoc.getString("username") ?: "Unknown"
                        val userName = userDoc.getString("username") ?: ""
                        val email = userDoc.getString("email") ?: ""

                        // Fetch member_info from subcollection
                        val memberInfoDoc = db.collection("teams")
                            .document(team.id)
                            .collection("members_info")
                            .document(memberId)
                            .get()
                            .await()

                        val position = memberInfoDoc.getString("position")
                        val ageGroup = memberInfoDoc.getString("ageGroup")

                        memberInfoList.add(
                            MemberDetail(
                                userId = memberId,
                                fullName = fullName,
                                userName = userName,
                                email = email,
                                isLeader = memberId == team.leaderId,
                                position = position,
                                ageGroup = ageGroup,
                                isCurrentUser = memberId == currentUserId
                            )
                        )
                    } catch (e: Exception) {
                        // Skip members that can't be loaded
                    }
                }

                // Sort: leader first, then current user, then alphabetically
                memberDetails = memberInfoList.sortedWith(
                    compareByDescending<MemberDetail> { it.isLeader }
                        .thenByDescending { it.isCurrentUser }
                        .thenBy { it.fullName }
                )

                isLoadingMembers = false
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load members", Toast.LENGTH_SHORT).show()
                isLoadingMembers = false
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Team Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F5F5)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InfoRow("Members", "${team.members.size}")
                    InfoRow("Last Meal Poll", team.lastMealPoll ?: "None")
                    ActivePollRow(hasActivePoll = team.currentlyOpenPoll != null)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Action Buttons
            if (isLeader) {
                ActionButton(
                    icon = Icons.Default.PlayArrow,
                    label = "Start New Poll",
                    onClick = onStartPoll,
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }

            if (team.currentlyOpenPoll != null) {
                ActionButton(
                    icon = Icons.Default.HowToVote,
                    label = "Join Active Poll",
                    onClick = onJoinPoll,
                    containerColor = Color(0xFF2196F3)
                )
            }

            // Team Members Section
            Spacer(Modifier.height(24.dp))

            TeamMembersSection(
                team = team,
                memberDetails = memberDetails,
                isLoadingMembers = isLoadingMembers,
                emailToAdd = emailToAdd,
                onEmailChange = { emailToAdd = it },
                isLeader = isLeader,
                currentUserId = currentUserId,
                onAddMember = { username ->
                    // Add member logic
                    if (username.isBlank()) {
                        Toast.makeText(context, "Please enter a username", Toast.LENGTH_SHORT).show()
                        return@TeamMembersSection
                    }

                    // Check if user already in team by userName (case-insensitive)
                    if (memberDetails.any { it.userName.equals(username.trim(), ignoreCase = true) }) {
                        Toast.makeText(context, "User already in team", Toast.LENGTH_SHORT).show()
                        return@TeamMembersSection
                    }

                    // Query Firestore to find user by username
                    db.collection("users")
                        .whereEqualTo("username", username.trim().lowercase())
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            if (querySnapshot.isEmpty) {
                                Toast.makeText(context, "User not found with that username", Toast.LENGTH_SHORT).show()
                            } else {
                                val userDoc = querySnapshot.documents[0]
                                val userId = userDoc.id

                                // Add user to team
                                db.collection("teams").document(team.id)
                                    .update("members", FieldValue.arrayUnion(userId))
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Member added successfully", Toast.LENGTH_SHORT).show()
                                        emailToAdd = ""
                                        refreshTrigger++  // Trigger reload
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "Failed to add member", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Error searching for user", Toast.LENGTH_SHORT).show()
                        }
                },
                onEditMember = { member ->
                    showEditMemberDialog = member
                },
                onRemoveMember = { member ->
                    showRemoveMemberDialog = member
                }
            )
        }

        // Edit Member Dialog
        showEditMemberDialog?.let { member ->
            EditMemberDialog(
                member = member,
                teamOccasionType = team.occasionType,
                onDismiss = { showEditMemberDialog = null },
                onSave = { position, ageGroup ->
                    // Update member_info subcollection
                    db.collection("teams")
                        .document(team.id)
                        .collection("members_info")
                        .document(member.userId)
                        .set(mapOf("position" to position, "ageGroup" to ageGroup))
                        .addOnSuccessListener {
                            Toast.makeText(context, "Member details updated", Toast.LENGTH_SHORT).show()
                            showEditMemberDialog = null
                            refreshTrigger++  // Trigger reload
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to update member", Toast.LENGTH_SHORT).show()
                        }
                }
            )
        }

        // Remove Member Dialog
        showRemoveMemberDialog?.let { member ->
            RemoveMemberDialog(
                member = member,
                onDismiss = { showRemoveMemberDialog = null },
                onConfirm = {
                    // Remove member from team
                    db.collection("teams").document(team.id)
                        .update("members", FieldValue.arrayRemove(member.userId))
                        .addOnSuccessListener {
                            Toast.makeText(context, "Member removed", Toast.LENGTH_SHORT).show()
                            showRemoveMemberDialog = null
                            refreshTrigger++  // Trigger reload
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to remove member", Toast.LENGTH_SHORT).show()
                        }
                }
            )
        }
    }

    @Composable
    private fun InfoRow(label: String, value: String) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color.Gray
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
        }
    }

    @Composable
    private fun ActivePollRow(hasActivePoll: Boolean) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Active Poll",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = if (hasActivePoll) MaterialTheme.colorScheme.primary else Color(0xFFF44336),
                            shape = CircleShape
                        )
                )
                Text(
                    text = if (hasActivePoll) "Active" else "Inactive",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
            }
        }
    }

    @Composable
    private fun ActionButton(
        icon: ImageVector,
        label: String,
        onClick: () -> Unit,
        containerColor: Color = MaterialTheme.colorScheme.primary
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 16.sp
            )
        }
    }

    @Composable
    private fun TeamMembersSection(
        team: Team,
        memberDetails: List<MemberDetail>,
        isLoadingMembers: Boolean,
        emailToAdd: String,
        onEmailChange: (String) -> Unit,
        isLeader: Boolean,
        currentUserId: String?,
        onAddMember: (String) -> Unit,
        onEditMember: (MemberDetail) -> Unit,
        onRemoveMember: (MemberDetail) -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Team Members",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Add Member Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = emailToAdd,
                    onValueChange = onEmailChange,
                    label = { Text("Enter username") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    prefix = { Text("@") }
                )
                IconButton(
                    onClick = { onAddMember(emailToAdd) },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.Add, "Add member", modifier = Modifier.size(24.dp))
                }
            }

            // Members List
            if (isLoadingMembers) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (memberDetails.isEmpty()) {
                Text(
                    "No members found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    memberDetails.forEach { member ->
                        MemberListItem(
                            member = member,
                            isLeader = isLeader,
                            currentUserId = currentUserId,
                            onEdit = { onEditMember(member) },
                            onRemove = { onRemoveMember(member) }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun MemberListItem(
        member: MemberDetail,
        isLeader: Boolean,
        currentUserId: String?,
        onEdit: () -> Unit,
        onRemove: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF5F5F5)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Member Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Name and badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = member.fullName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (member.isLeader) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    "Leader",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                        }
                        if (member.isCurrentUser) {
                            Surface(
                                color = Color.LightGray,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    "You",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 12.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }

                    // Position and age group
                    if (member.position != null || member.ageGroup != null) {
                        Text(
                            text = listOfNotNull(member.position, member.ageGroup).joinToString(" • "),
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Edit button (leader only, not for leader member)
                    if (isLeader && !member.isLeader) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, "Edit", tint = Color.Gray)
                        }
                    }

                    // Remove button
                    val canRemove = isLeader && !member.isLeader && !member.isCurrentUser
                    IconButton(
                        onClick = if (canRemove) onRemove else { {} },
                        enabled = canRemove
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            "Remove",
                            tint = if (canRemove) MaterialTheme.colorScheme.error else Color.LightGray
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun EditMemberDialog(
        member: MemberDetail,
        teamOccasionType: String,
        onDismiss: () -> Unit,
        onSave: (String, String) -> Unit
    ) {
        var selectedPosition by remember { mutableStateOf(member.position ?: "") }
        var selectedAgeGroup by remember { mutableStateOf(member.ageGroup ?: "") }
        var expandedPosition by remember { mutableStateOf(false) }
        var expandedAge by remember { mutableStateOf(false) }

        val positionOptions = getPositionOptions(teamOccasionType)
        val ageOptions = listOf("Child", "Teen", "Adult", "Senior")

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Edit Member Details") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Editing: ${member.fullName}", fontWeight = FontWeight.Bold)

                    // Position Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedPosition,
                        onExpandedChange = { expandedPosition = it }
                    ) {
                        OutlinedTextField(
                            value = selectedPosition,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Position") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPosition)
                            },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = expandedPosition,
                            onDismissRequest = { expandedPosition = false }
                        ) {
                            positionOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        selectedPosition = option
                                        expandedPosition = false
                                    }
                                )
                            }
                        }
                    }

                    // Age Group Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedAge,
                        onExpandedChange = { expandedAge = it }
                    ) {
                        OutlinedTextField(
                            value = selectedAgeGroup,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Age Group") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAge)
                            },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = expandedAge,
                            onDismissRequest = { expandedAge = false }
                        ) {
                            ageOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        selectedAgeGroup = option
                                        expandedAge = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { onSave(selectedPosition, selectedAgeGroup) }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    @Composable
    private fun RemoveMemberDialog(
        member: MemberDetail,
        onDismiss: () -> Unit,
        onConfirm: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Remove member?") },
            text = { Text("Are you sure you want to remove ${member.fullName} from this team?") },
            confirmButton = {
                TextButton(
                    onClick = onConfirm
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    private fun getPositionOptions(occasionType: String): List<String> {
        return when (occasionType) {
            "Formal Dinner with Clients" -> listOf("Host", "Client", "Colleague", "Support Staff", "Other")
            "Team Meeting" -> listOf("Manager", "Team Lead", "Team Member", "Intern", "Other")
            "Family Gathering" -> listOf("Parent", "Child", "Sibling", "Grandparent", "Relative", "Other")
            "Friends Gathering" -> listOf("Close Friend", "Acquaintance", "Plus One", "Other")
            "Birthday Celebration" -> listOf("Birthday Person", "Family", "Friend", "Colleague", "Other")
            "Romantic Date" -> listOf("Partner", "Date", "Other")
            else -> listOf("Guest", "Participant", "Other")
        }
    }

    private fun leaveTeam(team: Team) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("teams").document(team.id)
            .update("members", FieldValue.arrayRemove(uid))
            .addOnSuccessListener {
                Toast.makeText(this, "Left ${team.name}", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to leave team", Toast.LENGTH_SHORT).show()
            }
    }
}
