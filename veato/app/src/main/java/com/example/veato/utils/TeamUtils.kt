package com.example.veato.utils

import com.example.veato.Team

object TeamUtils {

    fun isTeamNameValid(name: String): Boolean {
        return name.trim().isNotEmpty()
    }

    fun buildTeam(name: String, leaderId: String): Team {
        return Team(
            id = "temp-id", // placeholder
            name = name.trim(),
            leaderId = leaderId,
            members = listOf(leaderId)
        )
    }
}