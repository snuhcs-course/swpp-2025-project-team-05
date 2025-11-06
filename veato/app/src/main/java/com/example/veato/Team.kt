package com.example.veato

data class Team(
    val id: String = "",
    val name: String = "",
    val leaderId: String = "",
    val members: List<String> = listOf(),
    val occasionType: String = "",
    val createdAt: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now(),
    val lastMealPoll: String? = null,
    val currentlyOpenPoll: String? = null
)

