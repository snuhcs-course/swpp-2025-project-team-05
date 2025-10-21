package com.example.veato.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class PortionSize(val displayName: String) {
    SMALL("Small"),
    MEDIUM("Medium"),
    LARGE("Large")
}
