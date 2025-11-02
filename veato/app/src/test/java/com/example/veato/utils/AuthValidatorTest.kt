package com.example.veato.utils

import org.junit.Assert.*
import org.junit.Test

class AuthValidatorTest {
    @Test
    fun `valid email and password returns true`() {
        assertTrue(AuthValidator.isLoginInputValid("user@gmail.com", "123456"))
    }

    @Test
    fun `empty email returns false`() {
        assertFalse(AuthValidator.isLoginInputValid("", "123456"))
    }

    @Test
    fun `empty password returns false`() {
        assertFalse(AuthValidator.isLoginInputValid("user@gmail.com", ""))
    }

    @Test
    fun `empty email and password returns false`() {
        assertFalse(AuthValidator.isLoginInputValid("", ""))
    }

    @Test
    fun `invalid email format returns false`() {
        assertFalse(AuthValidator.isLoginInputValid("user@@gmail", "123456"))
    }

    @Test
    fun `short password returns false`() {
        assertFalse(AuthValidator.isLoginInputValid("user@gmail.com", "123"))
    }

    @Test
    fun `valid reset email returns true`() {
        assertTrue(AuthValidator.isResetEmailValid("test@example.com"))
    }

    @Test
    fun `invalid reset email returns false`() {
        assertFalse(AuthValidator.isResetEmailValid("invalid_email"))
    }
}
