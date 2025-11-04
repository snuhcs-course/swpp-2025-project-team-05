package com.example.veato

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MembersAdapter(
    private val members: List<String>,
    private val leaderEmail: String,
    private val onRemove: (String) -> Unit,
    private val onEdit: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_LEADER = 0
    private val VIEW_TYPE_MEMBER = 1

    private val db = FirebaseFirestore.getInstance()

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
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email

        if (holder is LeaderViewHolder) {
            holder.email.text = email
        } else if (holder is MemberViewHolder) {
            holder.email.text = email
            holder.btnRemove.setOnClickListener { onRemove(email) }

            // Load dynamic member details (position + age)
            loadMemberDetails(email, holder.tvDetails)

            if (currentUserEmail == leaderEmail) {
                holder.btnEdit.visibility = View.VISIBLE
                holder.btnEdit.setOnClickListener { onEdit(email) }
            } else {
                holder.btnEdit.visibility = View.GONE
            }
        }
    }

    inner class LeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val email: TextView = view.findViewById(R.id.tvLeaderEmail)
    }

    inner class MemberViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val email: TextView = view.findViewById(R.id.tvMemberEmail)
        val tvDetails: TextView = view.findViewById(R.id.tvMemberDetails)
        val btnRemove: Button = view.findViewById(R.id.btnRemoveMember)
        val btnEdit: Button = view.findViewById(R.id.btnEditMember)
    }

    //Fetch position and ageGroup from Firestore → teams/{teamId}/members_info/{userId}
    private fun loadMemberDetails(email: String, textView: TextView) {
        db.collection("users").whereEqualTo("email", email).get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val userId = result.documents[0].id
                    val memberInfoRef = db.collection("teams")
                        .document(ViewMembersActivity.currentTeamId)
                        .collection("members_info")
                        .document(userId)

                    memberInfoRef.addSnapshotListener { infoDoc, _ ->
                        if (infoDoc != null && infoDoc.exists()) {
                            val position = infoDoc.getString("position") ?: ""
                            val ageGroup = infoDoc.getString("ageGroup") ?: ""

                            // Capitalize first letter for cleaner display
                            val formattedPosition = position.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase() else it.toString()
                            }
                            val formattedAge = ageGroup.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase() else it.toString()
                            }

                            val detailsText = buildString {
                                if (position.isNotEmpty()) append("Position: $formattedPosition\n")
                                if (ageGroup.isNotEmpty()) append("Age Group: $formattedAge")
                            }.trim()

                            textView.text = detailsText
                        } else {
                            textView.text = ""
                        }
                    }
                }
            }
    }
}
