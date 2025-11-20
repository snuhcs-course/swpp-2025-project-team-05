package com.example.veato.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class HeavinessLevelTest {

    @Test
    fun enums_haveCorrectDisplayNamesAndDescriptions() {
        assertEquals("Light", HeavinessLevel.LIGHT.displayName)
        assertEquals("Salads, soups, light meals", HeavinessLevel.LIGHT.description)

        assertEquals("Medium", HeavinessLevel.MEDIUM.displayName)
        assertEquals("Balanced meals", HeavinessLevel.MEDIUM.description)

        assertEquals("Heavy", HeavinessLevel.HEAVY.displayName)
        assertEquals("Fried, rich, filling meals", HeavinessLevel.HEAVY.description)
    }

    @Test
    fun heavinessLevel_enumHasThreeValues() {
        assertEquals(3, HeavinessLevel.entries.size)
    }
}
