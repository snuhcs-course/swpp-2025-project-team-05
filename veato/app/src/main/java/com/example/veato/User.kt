package com.example.veato.model

data class User(
    val uid: String = "",
    val fullName: String = "",
    val username: String = "", // stored without '@'
    val email: String = "",
    val createdAt: Long = 0,
    val position: String? = null,
    val ageGroup: String? = null
)