package com.example.veato.data.model

import org.junit.Assert.*
import org.junit.Test

class SoftPreferencesTest {

    @Test
    fun defaultSoftPreferences_hasNoPreferences() {
        val prefs = SoftPreferences.DEFAULT
        assertTrue(prefs.favoriteCuisines.isEmpty())
        assertEquals(SpiceLevel.MEDIUM, prefs.spiceTolerance)
        assertFalse(prefs.hasPreferences())
    }

    @Test
    fun favoriteCuisinesNotEmpty_hasPreferencesReturnsTrue() {
        val prefs = SoftPreferences(
            favoriteCuisines = listOf(CuisineType.KOREAN)
        )
        assertTrue(prefs.hasPreferences())
    }

    @Test
    fun nonDefaultSpiceTolerance_hasPreferencesReturnsTrue() {
        val prefs = SoftPreferences(spiceTolerance = SpiceLevel.HIGH)
        assertTrue(prefs.hasPreferences())
    }

    @Test
    fun bothPreferencesEmpty_hasPreferencesReturnsFalse() {
        val prefs = SoftPreferences()
        assertFalse(prefs.hasPreferences())
    }
}
