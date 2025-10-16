package com.example.veato

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

import android.util.Log
import android.widget.Toast

class ViewMembersActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var teamId: String
    private lateinit var adapter: MembersAdapter
    private val memberList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_members)

        teamId = intent.getStringExtra("teamId") ?: return

        val recycler = findViewById<RecyclerView>(R.id.recyclerMembers)
        val inputEmail = findViewById<EditText>(R.id.editEmail)
        val btnAdd = findViewById<Button>(R.id.btnAddMember)

        recycler.layoutManager = LinearLayoutManager(this)
        adapter = MembersAdapter(memberList) { email ->
            removeMemberByEmail(email)
        }
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
                memberList.clear()

                if (memberUids.isEmpty()) {
                    adapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                }

                // Fetch each user's email
                for (uid in memberUids) {
                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { userDoc ->
                            val email = userDoc.getString("email") ?: uid // fallback if missing
                            memberList.add(email)
                            adapter.notifyDataSetChanged()
                        }
                }
            }
    }

    private fun addMemberByEmail(email: String) {
        val teamRef = db.collection("teams").document(teamId)

        if (email.isBlank()) {
            Toast.makeText(this, "Please enter an email", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("DEBUG_AUTH", "Current UID: ${FirebaseAuth.getInstance().currentUser?.uid}")

        db.collection("users").whereEqualTo("email", email.trim()).get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val userDoc = result.documents[0]
                    val userId = userDoc.id

                    teamRef.update("members", FieldValue.arrayUnion(userId))
                        .addOnSuccessListener {
                            Toast.makeText(this, "Member added successfully!", Toast.LENGTH_SHORT).show()
                            loadMembers() // refresh list
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to add member: ${e.message}", Toast.LENGTH_LONG).show()
                            e.printStackTrace()
                        }
                } else {
                    Toast.makeText(this, "No user found with this email", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Query failed: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
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

}