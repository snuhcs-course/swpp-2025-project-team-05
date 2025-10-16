package com.example.veato

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CreateTeamActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_team)

        val teamNameInput = findViewById<EditText>(R.id.editTeamName)
        val btnCreate = findViewById<Button>(R.id.btnCreateTeam)

        btnCreate.setOnClickListener {
            val name = teamNameInput.text.toString().trim()
            val uid = auth.currentUser?.uid

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a team name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (uid == null) {
                Toast.makeText(this, "You must be logged in to create a team", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val teamRef = db.collection("teams").document()
            val team = Team(
                id = teamRef.id,
                name = name,
                leaderId = uid,
                members = listOf(uid)
            )

            teamRef.set(team)
                .addOnSuccessListener {
                    Toast.makeText(this, "Team '$name' created successfully!", Toast.LENGTH_SHORT).show()
                    finish()  // go back to MyTeamsActivity
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to create team: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}
