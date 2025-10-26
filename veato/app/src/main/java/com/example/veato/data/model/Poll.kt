package com.example.veato.data.model

data class Poll (
    val pollId: String = "",
    val pollTitle: String = "",
    val startTime: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now(),   // timestamp (millis)
    val duration: Int = 0,        // in seconds
    val teamId: String = "",
    val teamName: String = "",
    val isOpen: Boolean = false,
    val candidates: List<Candidate> = listOf()
)