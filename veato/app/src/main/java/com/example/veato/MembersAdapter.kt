package com.example.veato

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MembersAdapter(
    private val members: List<String>,
    private val leaderEmail: String,
    private val onRemove: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_LEADER = 0
    private val VIEW_TYPE_MEMBER = 1

    override fun getItemViewType(position: Int): Int {
        return if (members[position] == leaderEmail) VIEW_TYPE_LEADER else VIEW_TYPE_MEMBER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_LEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_leader, parent, false)
            LeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_member, parent, false)
            MemberViewHolder(view)
        }
    }

    override fun getItemCount() = members.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val email = members[position]
        if (holder is LeaderViewHolder) {
            holder.email.text = email
        } else if (holder is MemberViewHolder) {
            holder.email.text = email
            holder.btnRemove.setOnClickListener { onRemove(email) }
        }
    }

    inner class LeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val email: TextView = view.findViewById(R.id.tvLeaderEmail)
    }

    inner class MemberViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val email: TextView = view.findViewById(R.id.tvMemberEmail)
        val btnRemove: Button = view.findViewById(R.id.btnRemoveMember)
    }
}
