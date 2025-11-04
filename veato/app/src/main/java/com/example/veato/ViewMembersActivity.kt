package com.example.veato

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class ViewMembersActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var teamId: String
    private lateinit var adapter: MembersAdapter
    private val memberList = mutableListOf<String>()
    private var leaderEmail: String = ""

    companion object {
        lateinit var currentTeamId: String
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_members)

        teamId = intent.getStringExtra("teamId") ?: return
        currentTeamId = teamId

        val tvOccasionType = findViewById<TextView>(R.id.tvOccasionType)

        db.collection("teams").document(teamId).get()
            .addOnSuccessListener { doc ->
                val occasionType = doc.getString("occasionType") ?: "Other"
                tvOccasionType.text = occasionType
            }
            .addOnFailureListener {
                tvOccasionType.text = "Unknown"
            }

        val recycler = findViewById<RecyclerView>(R.id.recyclerMembers)
        val inputEmail = findViewById<EditText>(R.id.editEmail)
        val btnAdd = findViewById<Button>(R.id.btnAddMember)
        recycler.layoutManager = LinearLayoutManager(this)

        // Initialize adapter with placeholder leaderEmail (will refresh after loading)
        adapter = MembersAdapter(
            members = memberList,
            leaderEmail = leaderEmail,
            onRemove = { email -> removeMemberByEmail(email) },
            onEdit = { email -> showEditMemberDialog(email) }
        )
        recycler.adapter = adapter

        btnAdd.setOnClickListener {
            val email = inputEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter email", Toast.LENGTH_SHORT).show()
            } else {
                addMemberByEmail(email)
            }
        }

        loadMembers()
    }

    private fun loadMembers() {
        db.collection("teams").document(teamId).get()
            .addOnSuccessListener { doc ->
                val memberUids = doc.get("members") as? List<String> ?: listOf()
                val leaderId = doc.getString("leaderId") ?: ""

                if (memberUids.isEmpty()) {
                    memberList.clear()
                    adapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                }

                if (leaderId.isNotEmpty()) {
                    // Fetch leader email
                    db.collection("users").document(leaderId).get()
                        .addOnSuccessListener { leaderDoc ->
                            leaderEmail = leaderDoc.getString("email") ?: ""

                            val tempList = mutableListOf<String>()

                            // Fetch all member emails
                            for (uid in memberUids) {
                                db.collection("users").document(uid).get()
                                    .addOnSuccessListener { userDoc ->
                                        val email = userDoc.getString("email") ?: uid
                                        tempList.add(email)

                                        if (tempList.size == memberUids.size) {
                                            val sortedList = tempList.sortedWith(
                                                compareByDescending { it == leaderEmail }
                                            )
                                            memberList.clear()
                                            memberList.addAll(sortedList)

                                            // Refresh adapter with correct leader info
                                            val recycler = findViewById<RecyclerView>(R.id.recyclerMembers)
                                            adapter = MembersAdapter(
                                                members = memberList,
                                                leaderEmail = leaderEmail,
                                                onRemove = { email -> removeMemberByEmail(email) },
                                                onEdit = { email -> showEditMemberDialog(email) }
                                            )
                                            recycler.adapter = adapter
                                        }
                                    }
                            }
                        }
                } else {
                    // No leader ID fallback
                    memberList.clear()
                    for (uid in memberUids) {
                        db.collection("users").document(uid).get()
                            .addOnSuccessListener { userDoc ->
                                val email = userDoc.getString("email") ?: uid
                                memberList.add(email)
                                adapter.notifyDataSetChanged()
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load team: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun addMemberByEmail(email: String) {
        val teamRef = db.collection("teams").document(teamId)

        if (email.isBlank()) {
            Toast.makeText(this, "Please enter an email", Toast.LENGTH_SHORT).show()
            return
        }

        if (memberList.contains(email)) {
            Toast.makeText(this, "This member is already in the team", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users").whereEqualTo("email", email.trim()).get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val userDoc = result.documents[0]
                    val userId = userDoc.id

                    teamRef.update("members", FieldValue.arrayUnion(userId))
                        .addOnSuccessListener {
                            Toast.makeText(this, "Member added successfully!", Toast.LENGTH_SHORT).show()
                            loadMembers()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to add member: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(this, "No user found with this email", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Query failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun removeMemberByEmail(email: String) {
        val teamRef = db.collection("teams").document(teamId)

        db.collection("users").whereEqualTo("email", email.trim()).get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val userId = result.documents[0].id
                    teamRef.update("members", FieldValue.arrayRemove(userId))
                        .addOnSuccessListener {
                            Toast.makeText(this, "Member removed successfully!", Toast.LENGTH_SHORT).show()
                            loadMembers()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to remove member: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(this, "No user found with this email", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun getPositionsForOccasion(occasionType: String): List<String> {
        return when (occasionType) {
            "Family Gathering" -> listOf("Parent", "Child", "Grandparent", "Relative", "Sibling", "Cousin", "Guardian")
            "Formal Dinner with Clients" -> listOf("Host", "Manager", "Executive", "Client", "Partner", "Assistant", "Colleague")
            "Team meeting" -> listOf("Team Leader", "Manager", "Member", "Intern", "Supervisor", "Project Owner")
            "Friends Gathering" -> listOf("Organizer", "Friend", "Best Friend", "Guest", "Classmate", "Roommate")
            "Birthday Celebration" -> listOf("Birthday Person", "Guest", "Family Member", "Friend", "Organizer")
            "Romantic Date" -> listOf("Partner", "Boyfriend", "Girlfriend", "Spouse", "Fiancé/Fiancée")
            else -> listOf("Leader", "Manager", "Member", "Participant", "Guest", "Organizer")
        }
    }

    // Leader-only edit dialog for member position/age
    private fun showEditMemberDialog(email: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_member, null)
        val positionSpinner = dialogView.findViewById<Spinner>(R.id.spinnerPosition)
        val ageSpinner = dialogView.findViewById<Spinner>(R.id.spinnerAgeGroup)

        val ageOptions = listOf("Child", "Teen", "Adult", "Senior")
        val ageAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ageOptions)
        ageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ageSpinner.adapter = ageAdapter

        // Get the team’s occasion type to determine position list
        db.collection("teams").document(teamId).get()
            .addOnSuccessListener { teamDoc ->
                val occasionType = teamDoc.getString("occasionType") ?: "Other"
                val positionOptions = getPositionsForOccasion(occasionType)

                val positionAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, positionOptions)
                positionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                positionSpinner.adapter = positionAdapter
            }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit Member Info")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val selectedPosition = positionSpinner.selectedItem.toString()
                val selectedAge = ageSpinner.selectedItem.toString()
                updateMemberInfo(email, selectedPosition, selectedAge)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    // Update Firestore: teams/{teamId}/members_info/{userId}
    private fun updateMemberInfo(email: String, position: String, ageGroup: String) {
        db.collection("users").whereEqualTo("email", email).get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val userId = result.documents[0].id
                    val memberInfoRef = db.collection("teams")
                        .document(teamId)
                        .collection("members_info")
                        .document(userId)

                    val updates = mapOf(
                        "position" to position,
                        "ageGroup" to ageGroup
                    )

                    memberInfoRef.set(updates, SetOptions.merge())
                        .addOnSuccessListener {
                            Toast.makeText(this, "Member info updated!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "User not found for $email", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Query failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
