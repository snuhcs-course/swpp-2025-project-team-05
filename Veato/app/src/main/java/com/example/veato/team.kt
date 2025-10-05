package com.example.veato

import java.io.Serializable

data class Team(
    val teamName: String,
    val leaderID: String,
    val memberIDs: List<String>
) : Serializable
