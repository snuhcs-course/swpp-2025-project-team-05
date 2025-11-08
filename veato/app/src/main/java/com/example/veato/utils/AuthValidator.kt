package com.example.veato.utils

object AuthValidator {

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")

    fun isLoginInputValid(email: String, password: String): Boolean {
        if (email.isEmpty() || password.isEmpty()) return false
        if (!EMAIL_REGEX.matches(email)) return false
        if (password.length < 6) return false
        return true
    }

    fun isResetEmailValid(email: String): Boolean {
        if (email.isEmpty()) return false
        if (!EMAIL_REGEX.matches(email)) return false
        return true
    }
}
