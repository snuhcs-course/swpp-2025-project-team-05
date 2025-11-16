package com.example.veato

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TeamAdapter(
    private val teams: List<Team>,
    private val onTeamClick: (Team) -> Unit
) : RecyclerView.Adapter<TeamAdapter.TeamViewHolder>() {

    inner class TeamViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTeamName: TextView = view.findViewById(R.id.tvTeamName)
        val tvTeamInfo: TextView = view.findViewById(R.id.tvTeamInfo)
        val tvActivePoll: TextView = view.findViewById(R.id.tvActivePoll)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_team, parent, false)
        return TeamViewHolder(v)
    }

    override fun getItemCount() = teams.size

    override fun onBindViewHolder(holder: TeamViewHolder, position: Int) {
        val team = teams[position]

        // Display team info
        holder.tvTeamName.text = team.name
        val memberCount = team.members.size
        holder.tvTeamInfo.text = "$memberCount member${if (memberCount != 1) "s" else ""}"

        // Active poll indicator
        if (team.currentlyOpenPoll != null) {
            holder.tvActivePoll.visibility = View.VISIBLE
            holder.tvActivePoll.text = "● ACTIVE POLL"
        } else {
            holder.tvActivePoll.visibility = View.GONE
        }

        // Make entire card clickable
        holder.itemView.setOnClickListener {
            onTeamClick(team)
        }
    }
}
