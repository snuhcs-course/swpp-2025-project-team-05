package com.example.veato.utils

import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized


@RunWith(Parameterized::class)
class InvalidEmailParameterizedTest(
    private val inputEmail: String
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Invalid email: {0}")
        fun data(): List<Array<String>> = listOf(
            arrayOf("user@@gmail.com"),
            arrayOf("no-at-symbol.com"),
            arrayOf("@missingname.com"),
        )
    }

    @Test
    fun `given invalid email when validating then returns false`() {
        assertFalse(AuthValidator.isResetEmailValid(inputEmail))
    }
}
