package com.example.veato

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MyTeamsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val teamsList = mutableListOf<Team>()
    private lateinit var adapter: TeamAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_teams)

        val recycler = findViewById<RecyclerView>(R.id.recyclerTeams)
        recycler.layoutManager = LinearLayoutManager(this)

        adapter = TeamAdapter(
            teamsList,
            onViewMembers = { team -> viewMembers(team) },
            onStartPoll = { team -> startMealPoll(team) },
            onLeave = { team -> leaveTeam(team) }
        )

        recycler.adapter = adapter

        // Navigate to CreateTeamActivity
        findViewById<Button>(R.id.btnCreateTeam).setOnClickListener {
            startActivity(Intent(this, CreateTeamActivity::class.java))
        }

        loadTeams()
    }

    private fun loadTeams() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("teams")
            .whereArrayContains("members", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error loading teams", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                teamsList.clear()
                snapshot?.forEach { doc ->
                    val team = doc.toObject(Team::class.java).copy(id = doc.id)
                    teamsList.add(team)
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun viewMembers(team: Team) {
        val intent = Intent(this, ViewMembersActivity::class.java)
        intent.putExtra("teamId", team.id)
        startActivity(intent)
    }

    private fun startMealPoll(team: Team) {
        val intent = Intent(this, VoteSettingActivity::class.java)
        intent.putExtra("teamId", team.id)
        intent.putExtra("teamName", team.name)
        startActivity(intent)
    }

    private fun leaveTeam(team: Team) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("teams").document(team.id)
            .update("members", FieldValue.arrayRemove(uid))
            .addOnSuccessListener {
                Toast.makeText(this, "Left ${team.name}", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to leave team", Toast.LENGTH_SHORT).show()
            }
    }
}
