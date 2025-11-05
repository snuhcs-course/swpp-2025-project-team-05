package com.example.veato

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CreateTeamActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var occasionSpinner: Spinner
    private lateinit var selectedOccasion: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_team)

        val teamNameInput = findViewById<EditText>(R.id.editTeamName)
        val btnCreate = findViewById<Button>(R.id.btnCreateTeam)
        occasionSpinner = findViewById(R.id.spinnerOccasionType)

        // Occasion options
        val occasionOptions = listOf(
            "Select One...",
            "Family Gathering",
            "Formal Dinner with Clients",
            "Team Meeting",
            "Friends Gathering",
            "Birthday Celebration",
            "Romantic Date",
            "Other"
        )

        val adapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            occasionOptions
        ).also {
            it.setDropDownViewResource(R.layout.spinner_dropdown_item)
        }

        occasionSpinner.adapter = adapter

        // Spinner listener
        occasionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (position == 0) {
                    selectedOccasion = ""    // no valid selection yet
                } else {
                    selectedOccasion = occasionOptions[position]
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedOccasion = "Other"
            }
        }

        btnCreate.setOnClickListener {
            val name = teamNameInput.text.toString().trim()
            val uid = auth.currentUser?.uid

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a team name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedOccasion.isEmpty()) {
                Toast.makeText(this, "Please select an occasion type", Toast.LENGTH_SHORT).show()
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
                members = listOf(uid),
                occasionType = selectedOccasion   // 👈 new field added here
            )

            teamRef.set(team)
                .addOnSuccessListener {
                    Toast.makeText(this, "Team '$name' created successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to create team: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}
