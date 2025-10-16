package com.example.veato

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TeamAdapter(
    private val teams: List<Team>,
    private val onViewMembers: (Team) -> Unit,
    private val onStartPoll: (Team) -> Unit,
    private val onLeave: (Team) -> Unit
) : RecyclerView.Adapter<TeamAdapter.TeamViewHolder>() {

    inner class TeamViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvTeamName)
        val btnView: Button = view.findViewById(R.id.btnViewMembers)
        val btnPoll: Button = view.findViewById(R.id.btnStartPoll)
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
        holder.name.text = team.name

        holder.btnView.setOnClickListener { onViewMembers(team) }
        holder.btnPoll.setOnClickListener { onStartPoll(team) }
        holder.btnLeave.setOnClickListener { onLeave(team) }
    }
}
