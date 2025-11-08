package com.example.veato.model

import org.junit.Test
import com.google.common.truth.Truth.assertThat

class UserModelTest {
    @Test
    fun createUser_success() {
        val user = User(
            uid = "123",
            fullName = "John Doe",
            email = "john@example.com"
        )

        assertThat(user.uid).isEqualTo("123")
        assertThat(user.fullName).contains("John")
    }

    @Test
    fun user_equalityCheck_worksCorrectly() {
        val u1 = User("1", "Alice", "alice@example.com")
        val u2 = User("1", "Alice", "alice@example.com")
        assertThat(u1).isEqualTo(u2)
    }
}

