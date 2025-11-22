package com.example.veato.data.model

import org.junit.Assert.*
import org.junit.Test

class SpiceLevelTest {
    @Test
    fun fromLevel_returnsCorrectEnum() {
        assertEquals(SpiceLevel.NONE, SpiceLevel.fromLevel(1))
        assertEquals(SpiceLevel.LOW, SpiceLevel.fromLevel(2))
        assertEquals(SpiceLevel.MEDIUM, SpiceLevel.fromLevel(3))
        assertEquals(SpiceLevel.HIGH, SpiceLevel.fromLevel(4))
        assertEquals(SpiceLevel.EXTRA, SpiceLevel.fromLevel(5))
    }

    @Test
    fun fromLevel_withInvalidLevel_returnsMedium() {
        assertEquals(SpiceLevel.MEDIUM, SpiceLevel.fromLevel(999))
        assertEquals(SpiceLevel.MEDIUM, SpiceLevel.fromLevel(-5))
    }

    @Test
    fun spiceLevel_properties_areCorrect() {
        assertEquals(3, SpiceLevel.MEDIUM.level)
        assertEquals("Medium Spice Preferred", SpiceLevel.MEDIUM.displayName)
    }
}
