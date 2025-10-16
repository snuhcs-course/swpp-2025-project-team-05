package com.example.veato

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth

class TeamAdapter(
    private val teams: List<Team>,
    private val onViewMembers: (Team) -> Unit,
    private val onStartPoll: (Team) -> Unit,
    private val onLeave: (Team) -> Unit
) : RecyclerView.Adapter<TeamAdapter.TeamViewHolder>() {

    inner class TeamViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTeamName: TextView = view.findViewById(R.id.tvTeamName)
        val tvTeamInfo: TextView = view.findViewById(R.id.tvTeamInfo)
        val btnViewMembers: Button = view.findViewById(R.id.btnViewMembers)
        val btnStartPoll: Button = view.findViewById(R.id.btnStartPoll)
        val btnLeave: Button = view.findViewById(R.id.btnLeave)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_team, parent, false)
        return TeamViewHolder(v)
    }

    override fun getItemCount() = teams.size

    override fun onBindViewHolder(holder: TeamViewHolder, position: Int) {
        val team = teams[position]
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        // Dynamic display
        holder.tvTeamName.text = team.name
        val memberCount = team.members.size
        val lastMeal = team.lastMealPoll ?: "No record yet"
        holder.tvTeamInfo.text = "$memberCount members     • Last: $lastMeal"

        // Button actions
        holder.btnViewMembers.setOnClickListener { onViewMembers(team) }
        holder.btnStartPoll.setOnClickListener { onStartPoll(team) }
        holder.btnLeave.setOnClickListener { onLeave(team) }

        // Role-based visibility
        if (team.leaderId == currentUserId) {
            holder.btnStartPoll.visibility = View.VISIBLE
            holder.btnLeave.visibility = View.GONE
        } else {
            holder.btnStartPoll.visibility = View.GONE
            holder.btnLeave.visibility = View.VISIBLE
        }
    }

}
